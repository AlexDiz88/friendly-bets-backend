package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.MatchSchedulesUpdatedEvent;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.MatchFinalizeOrchestrator;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * Date-driven LIVE polling: one HTTP request per UTC kickoff date while any match is tracked.
 * First tick at {@code utc_kickoff}, then every poll interval until all tracked matches finish.
 */
@Component
public class TwentyFourScoreLiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreLiveScheduler.class);

    private final RunningSeasonLookup runningSeasonLookup;
    private final LayerProviderRouter router;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalDataLayerConfigService layerConfigService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final TwentyFourScoreProperties properties;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> nextFuture;

    public TwentyFourScoreLiveScheduler(
            RunningSeasonLookup runningSeasonLookup,
            LayerProviderRouter router,
            MatchFinalizeOrchestrator matchFinalizeOrchestrator,
            ExternalDataLayerConfigService layerConfigService,
            MatchScheduleRepository matchScheduleRepository,
            TwentyFourScoreProperties properties,
            @Qualifier("twentyFourScoreLiveTaskScheduler") ThreadPoolTaskScheduler taskScheduler
    ) {
        this.runningSeasonLookup = runningSeasonLookup;
        this.router = router;
        this.matchFinalizeOrchestrator = matchFinalizeOrchestrator;
        this.layerConfigService = layerConfigService;
        this.matchScheduleRepository = matchScheduleRepository;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        rescheduleFromKickoffs(false);
    }

    @EventListener(MatchSchedulesUpdatedEvent.class)
    public void onMatchSchedulesUpdated(MatchSchedulesUpdatedEvent ignored) {
        rescheduleFromKickoffs(false);
    }

    /** Recalculate next wake from DB kickoffs (poke after SCHEDULE / startup). */
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
            log.warn("24score LIVE tick failed: {}", e.getMessage());
        } finally {
            running.set(false);
            try {
                scheduleNext(computeNextWake(Instant.now(), true));
            } catch (RuntimeException e) {
                log.warn("24score LIVE reschedule failed: {}", e.getMessage());
                scheduleNext(Optional.of(Instant.now().plusMillis(Math.max(1_000L, properties.getSchedulerTickMs()))));
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
                .anyMatch(s -> TwentyFourScoreLiveSupport.isLiveHttpCandidate(s, now));
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
                log.info("24score LIVE tracked={} http={} updated={} finished={} dates={} pendingFull={}",
                        result.trackedCount(),
                        result.httpRequests(),
                        result.updated(),
                        result.finishedDetected(),
                        result.datesSynced(),
                        result.pendingFullMatchIds().size());
            }
            pendingFull.addAll(result.pendingFullMatchIds());
        } catch (RuntimeException e) {
            log.warn("24score LIVE sync failed: {}", e.getMessage());
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
            if (TwentyFourScoreLiveSupport.needsFullMatch(schedule) && schedule.getId() != null) {
                pendingFull.add(schedule.getId());
            }
        }
        return pendingFull;
    }

    /**
     * @param afterPoll if true, active matches wait for the poll interval instead of firing immediately
     */
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
        Instant nearestKickoff = null;

        for (MatchSchedule schedule : matchScheduleRepository.findBySeasonId(seasonId)) {
            if (TwentyFourScoreLiveSupport.isLiveHttpCandidate(schedule, now)
                    || TwentyFourScoreLiveSupport.needsFullMatch(schedule)) {
                hasActiveOrPendingFull = true;
            }
            Instant kickoff = TwentyFourScoreLiveSupport.upcomingKickoffOrNull(schedule, now);
            if (kickoff != null && (nearestKickoff == null || kickoff.isBefore(nearestKickoff))) {
                nearestKickoff = kickoff;
            }
        }

        if (hasActiveOrPendingFull) {
            if (afterPoll) {
                Instant afterInterval = now.plusMillis(Math.max(1_000L, properties.getSchedulerTickMs()));
                if (nearestKickoff != null && nearestKickoff.isBefore(afterInterval)) {
                    return Optional.of(nearestKickoff);
                }
                return Optional.of(afterInterval);
            }
            return Optional.of(now);
        }
        if (nearestKickoff != null) {
            return Optional.of(nearestKickoff);
        }
        return Optional.empty();
    }

    private synchronized void scheduleNext(Optional<Instant> when) {
        if (nextFuture != null) {
            nextFuture.cancel(false);
            nextFuture = null;
        }
        if (when.isEmpty()) {
            log.debug("24score LIVE dormant (no upcoming kickoff / active matches)");
            return;
        }
        Instant fireAt = when.get().isBefore(Instant.now()) ? Instant.now() : when.get();
        nextFuture = taskScheduler.schedule(this::safeTick, fireAt);
        log.debug("24score LIVE next wake at {}", fireAt);
    }
}
