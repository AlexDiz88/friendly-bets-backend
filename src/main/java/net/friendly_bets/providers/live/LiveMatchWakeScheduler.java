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
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Layer-level LIVE wake: first tick at {@code utc_kickoff}, then every poll interval
 * while any match is tracked. Calls {@link LayerProviderRouter} (primary → secondary) —
 * not a specific physical API.
 */
@Component
public class LiveMatchWakeScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchWakeScheduler.class);

    private final RunningSeasonLookup runningSeasonLookup;
    private final LayerProviderRouter router;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalDataLayerConfigService layerConfigService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchResultSyncProperties matchResultSyncProperties;
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
            @Qualifier("liveMatchWakeTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            @Value("${external-data.layers.LIVE.scheduler-tick-ms:300000}") long schedulerTickMs
    ) {
        this.runningSeasonLookup = runningSeasonLookup;
        this.router = router;
        this.matchFinalizeOrchestrator = matchFinalizeOrchestrator;
        this.layerConfigService = layerConfigService;
        this.matchScheduleRepository = matchScheduleRepository;
        this.matchResultSyncProperties = matchResultSyncProperties;
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
        LinkedHashSet<String> pendingFull = collectPendingFullMatchIds(season.getId());

        boolean hasTracked = matchScheduleRepository.findBySeasonId(season.getId()).stream()
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

        if (layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH) && !pendingFull.isEmpty()) {
            try {
                matchFinalizeOrchestrator.finalizePendingFullMatches(new ArrayList<>(pendingFull));
            } catch (RuntimeException fullEx) {
                log.warn("FULL after LIVE failed: {}", fullEx.getMessage());
            }
        }
    }

    private LinkedHashSet<String> collectPendingFullMatchIds(String seasonId) {
        LinkedHashSet<String> pendingFull = new LinkedHashSet<>();
        for (MatchSchedule schedule : matchScheduleRepository.findBySeasonId(seasonId)) {
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
                Instant afterInterval = now.plusMillis(schedulerTickMs);
                if (nearestWake != null && nearestWake.isBefore(afterInterval)) {
                    return Optional.of(nearestWake.isBefore(now) ? now : nearestWake);
                }
                return Optional.of(afterInterval);
            }
            return Optional.of(now);
        }
        if (nearestWake != null) {
            return Optional.of(nearestWake.isBefore(now) ? now : nearestWake);
        }
        return Optional.empty();
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
