package net.friendly_bets.providers.live;

import net.friendly_bets.models.AppSettings;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataLayersUpdatedEvent;
import net.friendly_bets.providers.FullMatchAttemptSupport;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.MatchSchedulesUpdatedEvent;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalDataLayerAutoDisableService;
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

import java.time.Duration;
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
 * <p><b>HTTP only when there is work:</b> {@link #tick} calls {@code syncLive} only if there
 * is at least one LIVE candidate or pending FULL. Empty re-eval ticks do not hit the network.
 *
 * <p><b>Never permanently dormant while LIVE is enabled:</b> if there are no candidates and
 * no upcoming kickoff yet (gap between matchdays / missed SCHEDULE event), a re-eval
 * heartbeat keeps the scheduler alive so the next kickoff cannot be missed. True idle
 * ({@link Optional#empty()}) only when {@code LIVE.enabled=false}.
 *
 * <p>After circuit-breaker disable / admin layer PATCH, {@link ExternalDataLayersUpdatedEvent}
 * reschedules (re-enable must not leave wake cancelled forever).
 */
@Component
public class LiveMatchWakeScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchWakeScheduler.class);

    /**
     * Re-evaluate upcoming kickoffs at least this often (recovery from missed one-shot wakes
     * and from empty gaps between matchdays). Not an HTTP poll interval.
     */
    static final long WAKE_HEARTBEAT_MS = 900_000L;

    /** Short retry when a tick overlaps another and the chain would otherwise be lost. */
    static final long OVERLAP_RETRY_MS = 5_000L;

    private final RunningSeasonLookup runningSeasonLookup;
    private final LayerProviderRouter router;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalDataLayerConfigService layerConfigService;
    private final ExternalDataLayerAutoDisableService layerAutoDisableService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchResultSyncProperties matchResultSyncProperties;
    private final LiveMatchSyncDiagnostics liveMatchSyncDiagnostics;
    private final LiveMatchSecondaryCatchUp liveMatchSecondaryCatchUp;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final long schedulerTickMs;
    private final Duration circuitReenableAfter;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> nextFuture;

    public LiveMatchWakeScheduler(
            RunningSeasonLookup runningSeasonLookup,
            LayerProviderRouter router,
            MatchFinalizeOrchestrator matchFinalizeOrchestrator,
            ExternalDataLayerConfigService layerConfigService,
            ExternalDataLayerAutoDisableService layerAutoDisableService,
            MatchScheduleRepository matchScheduleRepository,
            MatchResultSyncProperties matchResultSyncProperties,
            LiveMatchSyncDiagnostics liveMatchSyncDiagnostics,
            LiveMatchSecondaryCatchUp liveMatchSecondaryCatchUp,
            @Qualifier("liveMatchWakeTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            @Value("${external-data.layers.LIVE.scheduler-tick-ms:300000}") long schedulerTickMs,
            @Value("${external-data.circuit-breaker.reenable-after-hours:6}") long circuitReenableAfterHours
    ) {
        this.runningSeasonLookup = runningSeasonLookup;
        this.router = router;
        this.matchFinalizeOrchestrator = matchFinalizeOrchestrator;
        this.layerConfigService = layerConfigService;
        this.layerAutoDisableService = layerAutoDisableService;
        this.matchScheduleRepository = matchScheduleRepository;
        this.matchResultSyncProperties = matchResultSyncProperties;
        this.liveMatchSyncDiagnostics = liveMatchSyncDiagnostics;
        this.liveMatchSecondaryCatchUp = liveMatchSecondaryCatchUp;
        this.taskScheduler = taskScheduler;
        this.schedulerTickMs = Math.max(1_000L, schedulerTickMs);
        this.circuitReenableAfter = Duration.ofHours(Math.max(1L, circuitReenableAfterHours));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        ensureLiveEnabled();
        rescheduleFromKickoffs(false);
    }

    @EventListener(MatchSchedulesUpdatedEvent.class)
    public void onMatchSchedulesUpdated(MatchSchedulesUpdatedEvent ignored) {
        rescheduleFromKickoffs(false);
    }

    @EventListener(ExternalDataLayersUpdatedEvent.class)
    public void onExternalDataLayersUpdated(ExternalDataLayersUpdatedEvent ignored) {
        rescheduleFromKickoffs(false);
    }

    public void rescheduleFromKickoffs(boolean afterPoll) {
        WakePlan plan = computeNextWake(Instant.now(), afterPoll);
        scheduleNext(plan);
    }

    /**
     * After admin LIVE/FULL sync: ensure LIVE auto-sync is on, then re-arm wake.
     */
    public void armAfterManualSync() {
        ensureLiveEnabled();
        rescheduleFromKickoffs(false);
    }

    /**
     * If Mongo has LIVE.enabled=false (admin toggle / legacy), turn it back on.
     * Respects circuit cooldown — do not undo {@code layerCircuitOpen} before auto-re-enable.
     */
    private void ensureLiveEnabled() {
        if (layerAutoDisableService.isCircuitCooldownActive(ExternalDataLayer.LIVE, circuitReenableAfter)) {
            log.info("LIVE circuit cooldown active — skip ensureLayerEnabled");
            return;
        }
        if (layerConfigService.ensureLayerEnabled(ExternalDataLayer.LIVE)) {
            log.warn("LIVE.enabled was false — set to true (auto-sync restored)");
        }
    }

    private void safeTick() {
        if (!running.compareAndSet(false, true)) {
            armOverlapRetry();
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
                scheduleNext(WakePlan.at(
                        Instant.now().plusMillis(schedulerTickMs),
                        "reschedule failure fallback"));
            }
        }
    }

    /**
     * Another tick is in progress. If its future was already cancelled and nothing is armed,
     * schedule a short retry so the chain cannot die on overlap.
     */
    private synchronized void armOverlapRetry() {
        if (nextFuture != null && !nextFuture.isDone() && !nextFuture.isCancelled()) {
            return;
        }
        Instant retryAt = Instant.now().plusMillis(OVERLAP_RETRY_MS);
        nextFuture = taskScheduler.schedule(this::safeTick, retryAt);
        log.warn("LIVE wake overlap; retry at {}", retryAt);
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
        AppSettings.LayerAssignment liveAssignment = layerConfigService.assignment(ExternalDataLayer.LIVE);
        String livePrimary = liveAssignment != null ? liveAssignment.getPrimaryProvider() : null;
        liveMatchSyncDiagnostics.reportNeverPolledAfterKickoff(season.getId(), seasonSchedules, now, livePrimary);

        boolean hasTracked = seasonSchedules.stream()
                .anyMatch(s -> LiveMatchSupport.isLiveHttpCandidate(s, now));
        boolean hasPendingFull = seasonSchedules.stream().anyMatch(LiveMatchSupport::needsFullMatch);
        if (!hasTracked && !hasPendingFull) {
            // Re-eval only — no HTTP. Scheduler stays armed via computeNextWake heartbeat.
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
            });
        } catch (RuntimeException e) {
            log.warn("LIVE secondary catch-up failed: {}", e.getMessage());
        }

        // Collect FULL targets only after LIVE may have corrected a false FINISHED.
        List<MatchSchedule> afterLive = matchScheduleRepository.findBySeasonId(season.getId());
        LinkedHashSet<String> pendingFull = collectPendingFullMatchIds(afterLive);
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

    WakePlan computeNextWake(Instant now, boolean afterPoll) {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.LIVE)) {
            return WakePlan.idle("layer disabled");
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty()) {
            return WakePlan.at(reevalAt(now), "no running season");
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
                return WakePlan.at(
                        nextWakeAfterPoll(now, nearestWake, schedulerTickMs),
                        "active poll interval");
            }
            return WakePlan.at(now, "active immediate");
        }
        if (nearestWake != null) {
            Instant target = nearestWake.isBefore(now) ? now : nearestWake;
            return WakePlan.at(
                    capWakeByHeartbeat(now, target, WAKE_HEARTBEAT_MS),
                    "await kickoff/full due");
        }
        // Enabled, season exists, but no kickoffs yet — keep re-eval so SCHEDULE gaps cannot
        // leave LIVE dead until the next deploy/restart.
        return WakePlan.at(reevalAt(now), "await schedule/kickoffs");
    }

    static Instant reevalAt(Instant now) {
        return now.plusMillis(Math.max(1_000L, WAKE_HEARTBEAT_MS));
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

    private synchronized void scheduleNext(WakePlan plan) {
        if (nextFuture != null) {
            nextFuture.cancel(false);
            nextFuture = null;
        }
        if (plan == null || plan.when().isEmpty()) {
            log.info("LIVE wake idle ({})", plan != null ? plan.reason() : "null plan");
            return;
        }
        Instant fireAt = plan.when().get().isBefore(Instant.now()) ? Instant.now() : plan.when().get();
        nextFuture = taskScheduler.schedule(this::safeTick, fireAt);
        log.info("LIVE wake next at {} ({})", fireAt, plan.reason());
    }

    /**
     * Next wake instant + reason for logs. {@code when} empty = true idle (layer disabled).
     */
    record WakePlan(Optional<Instant> when, String reason) {
        static WakePlan idle(String reason) {
            return new WakePlan(Optional.empty(), reason);
        }

        static WakePlan at(Instant when, String reason) {
            return new WakePlan(Optional.of(when), reason);
        }
    }
}
