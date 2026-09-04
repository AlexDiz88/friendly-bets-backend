package net.friendly_bets.providers.live;

import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchAttemptSupport;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.MatchSchedulesUpdatedEvent;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.MatchFinalizeOrchestrator;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Layer-level LIVE wake: first tick at {@code utc_kickoff}, then every poll interval
 * while any match is tracked. Calls {@link LayerProviderRouter} (primary → secondary) —
 * not a specific physical API.
 *
 * <p>Long waits until a distant kickoff are capped by a heartbeat so a lost
 * {@link ScheduledFuture} (deploy, cancel race) cannot leave the layer dormant
 * while kickoffs pass silently.
 */
@Component
public class LiveMatchWakeScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchWakeScheduler.class);

    /** Re-evaluate upcoming kickoffs at least this often (recovery from missed one-shot wakes). */
    static final long WAKE_HEARTBEAT_MS = 900_000L;

    private final RunningSeasonLookup runningSeasonLookup;
    private final LayerProviderRouter router;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalDataLayerConfigService layerConfigService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchResultSyncProperties matchResultSyncProperties;
    private final LiveMatchSyncDiagnostics liveMatchSyncDiagnostics;
    private final LiveMatchSecondaryCatchUp liveMatchSecondaryCatchUp;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final long schedulerTickMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> nextFuture;

    public LiveMatchWakeScheduler(
            RunningSeasonLookup runningSeasonLookup,
            LayerProviderRouter router,
            MatchFinalizeOrchestrator matchFinalizeOrchestrator,
            ExternalDataLayerConfigService layerConfigService,
            MatchScheduleRepository matchScheduleRepository,
            MatchResultSyncProperties matchResultSyncProperties,
            LiveMatchSyncDiagnostics liveMatchSyncDiagnostics,
            LiveMatchSecondaryCatchUp liveMatchSecondaryCatchUp,
            @Qualifier("liveMatchWakeTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            @Value("${external-data.layers.LIVE.scheduler-tick-ms:300000}") long schedulerTickMs
    ) {
        this.runningSeasonLookup = runningSeasonLookup;
        this.router = router;
        this.matchFinalizeOrchestrator = matchFinalizeOrchestrator;
        this.layerConfigService = layerConfigService;
        this.matchScheduleRepository = matchScheduleRepository;
        this.matchResultSyncProperties = matchResultSyncProperties;
        this.liveMatchSyncDiagnostics = liveMatchSyncDiagnostics;
        this.liveMatchSecondaryCatchUp = liveMatchSecondaryCatchUp;
        this.taskScheduler = taskScheduler;
        this.schedulerTickMs = Math.max(1_000L, schedulerTickMs);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        rescheduleFromKickoffs(false);
    }

    @EventListener(MatchSchedulesUpdatedEvent.class)
    public void onMatchSchedulesUpdated(MatchSchedulesUpdatedEvent ignored) {
        rescheduleFromKickoffs(false);
    }

    public void rescheduleFromKickoffs(boolean afterPoll) {
        scheduleNext(computeNextWake(Instant.now(), afterPoll));
    }

    private void safeTick() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            tick();
        } catch (RuntimeException e) {
            log.warn("LIVE wake tick failed: {}", e.getMessage());
        } finally {
            running.set(false);
            try {
                scheduleNext(computeNextWake(Instant.now(), true));
            } catch (RuntimeException e) {
                log.warn("LIVE wake reschedule failed: {}", e.getMessage());
                scheduleNext(Optional.of(Instant.now().plusMillis(schedulerTickMs)));
            }
        }
    }

    private void tick() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.LIVE)) {
            return;
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty()) {
            return;
        }
        Season season = seasonOpt.get();
        Instant now = Instant.now();
        List<MatchSchedule> seasonSchedules = matchScheduleRepository.findBySeasonId(season.getId());
        liveMatchSyncDiagnostics.reportNeverPolledAfterKickoff(season.getId(), seasonSchedules, now);

        LinkedHashSet<String> pendingFull = collectPendingFullMatchIds(seasonSchedules);

        boolean hasTracked = seasonSchedules.stream()
                .anyMatch(s -> LiveMatchSupport.isLiveHttpCandidate(s, now));
        if (!hasTracked && pendingFull.isEmpty()) {
            return;
        }

        try {
            LiveMatchProvider.LiveSyncResult result = router.execute(
                    ExternalDataLayer.LIVE,
                    LiveMatchProvider.class,
                    p -> p.syncLive(season),
                    "ALL"
            );
            if (result.updated() > 0 || result.finishedDetected() > 0
                    || !result.pendingFullMatchIds().isEmpty()) {
                log.info("LIVE tracked={} http={} updated={} finished={} dates={} pendingFull={}",
                        result.trackedCount(),
                        result.httpRequests(),
                        result.updated(),
                        result.finishedDetected(),
                        result.datesSynced(),
                        result.pendingFullMatchIds().size());
            }
            pendingFull.addAll(result.pendingFullMatchIds());
        } catch (RuntimeException e) {
            log.warn("LIVE sync failed: {}", e.getMessage());
        }

        // Re-read after primary: overdue non-terminal → independent poll via LIVE secondary.
        List<MatchSchedule> afterPrimary = matchScheduleRepository.findBySeasonId(season.getId());
        try {
            liveMatchSecondaryCatchUp.catchUpIfNeeded(season, afterPrimary, Instant.now()).ifPresent(secondaryResult -> {
                if (secondaryResult.updated() > 0 || secondaryResult.finishedDetected() > 0
                        || !secondaryResult.pendingFullMatchIds().isEmpty()) {
                    log.info("LIVE secondary catch-up http={} updated={} finished={} pendingFull={}",
                            secondaryResult.httpRequests(),
                            secondaryResult.updated(),
                            secondaryResult.finishedDetected(),
                            secondaryResult.pendingFullMatchIds().size());
                }
                pendingFull.addAll(secondaryResult.pendingFullMatchIds());
            });
        } catch (RuntimeException e) {
            log.warn("LIVE secondary catch-up failed: {}", e.getMessage());
        }

        if (layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH) && !pendingFull.isEmpty()) {
            try {
                matchFinalizeOrchestrator.finalizePendingFullMatches(new ArrayList<>(pendingFull));
            } catch (RuntimeException fullEx) {
                log.warn("FULL after LIVE failed: {}", fullEx.getMessage());
            }
        }
    }

    private LinkedHashSet<String> collectPendingFullMatchIds(List<MatchSchedule> schedules) {
        LinkedHashSet<String> pendingFull = new LinkedHashSet<>();
        for (MatchSchedule schedule : schedules) {
            if (LiveMatchSupport.needsFullMatch(schedule) && schedule.getId() != null) {
                pendingFull.add(schedule.getId());
            }
        }
        return pendingFull;
    }

    Optional<Instant> computeNextWake(Instant now, boolean afterPoll) {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.LIVE)) {
            return Optional.empty();
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty()) {
            return Optional.empty();
        }
        String seasonId = seasonOpt.get().getId();
        boolean hasActiveOrPendingFull = false;
        Instant nearestWake = null;

        for (MatchSchedule schedule : matchScheduleRepository.findBySeasonId(seasonId)) {
            if (LiveMatchSupport.isLiveHttpCandidate(schedule, now)
                    || LiveMatchSupport.needsFullMatch(schedule)) {
                hasActiveOrPendingFull = true;
            }
            Instant kickoff = LiveMatchSupport.upcomingKickoffOrNull(schedule, now);
            if (kickoff != null && (nearestWake == null || kickoff.isBefore(nearestWake))) {
                nearestWake = kickoff;
            }
            if (LiveMatchSupport.needsFullMatch(schedule)) {
                Instant fullDue = FullMatchAttemptSupport.resolveDueAt(schedule, matchResultSyncProperties);
                if (fullDue != null && (nearestWake == null || fullDue.isBefore(nearestWake))) {
                    nearestWake = fullDue;
                }
            }
        }

        if (hasActiveOrPendingFull) {
            if (afterPoll) {
                return Optional.of(nextWakeAfterPoll(now, nearestWake, schedulerTickMs));
            }
            return Optional.of(now);
        }
        if (nearestWake != null) {
            Instant target = nearestWake.isBefore(now) ? now : nearestWake;
            return Optional.of(capWakeByHeartbeat(now, target, WAKE_HEARTBEAT_MS));
        }
        return Optional.empty();
    }

    /**
     * After a poll, never fire immediately even if FULL due-at is already in the past
     * (failed attempt without deferral used to tight-loop every 2–3s).
     */
    static Instant nextWakeAfterPoll(Instant now, Instant nearestWake, long schedulerTickMs) {
        Instant afterInterval = now.plusMillis(Math.max(1_000L, schedulerTickMs));
        Instant soonestFuture = nearestWake != null && !nearestWake.isBefore(now) ? nearestWake : null;
        if (soonestFuture != null && soonestFuture.isBefore(afterInterval)) {
            return soonestFuture;
        }
        return afterInterval;
    }

    /**
     * Do not sleep longer than {@code heartbeatMs} while waiting for a distant kickoff —
     * otherwise a cancelled/missed one-shot {@link ScheduledFuture} leaves LIVE dormant.
     */
    static Instant capWakeByHeartbeat(Instant now, Instant target, long heartbeatMs) {
        Instant cap = now.plusMillis(Math.max(1_000L, heartbeatMs));
        if (target == null || target.isBefore(now)) {
            return now;
        }
        return target.isBefore(cap) ? target : cap;
    }

    private synchronized void scheduleNext(Optional<Instant> when) {
        if (nextFuture != null) {
            nextFuture.cancel(false);
            nextFuture = null;
        }
        if (when.isEmpty()) {
            log.debug("LIVE wake dormant (no upcoming kickoff / active matches)");
            return;
        }
        Instant fireAt = when.get().isBefore(Instant.now()) ? Instant.now() : when.get();
        nextFuture = taskScheduler.schedule(this::safeTick, fireAt);
        log.debug("LIVE wake next at {}", fireAt);
    }
}
