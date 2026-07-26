package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.League;
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
 * Kickoff-driven LIVE polling: first HTTP at {@code utc_kickoff}, then every poll interval
 * while matches are in play; stop per match when finished. No monitoring when idle.
 * Reschedule after SCHEDULE via {@link MatchSchedulesUpdatedEvent}.
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
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return;
        }
        Season season = seasonOpt.get();
        Instant now = Instant.now();
        LinkedHashSet<String> pendingFull = new LinkedHashSet<>();

        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null || league.getId() == null) {
                continue;
            }
            if (!TwentyFourScoreLeagueTitles.supported().contains(league.getLeagueCode())) {
                continue;
            }
            List<MatchSchedule> schedules = matchScheduleRepository.findByLeagueIdAndSeasonId(
                    league.getId(), season.getId());
            boolean hasHttp = schedules.stream()
                    .anyMatch(s -> TwentyFourScoreLiveSupport.isLiveHttpCandidate(s, now));
            for (MatchSchedule schedule : schedules) {
                if (TwentyFourScoreLiveSupport.needsFullMatch(schedule) && schedule.getId() != null) {
                    pendingFull.add(schedule.getId());
                }
            }
            if (!hasHttp) {
                continue;
            }
            try {
                LiveMatchProvider.LiveSyncResult result = router.execute(
                        ExternalDataLayer.LIVE,
                        LiveMatchProvider.class,
                        p -> p.syncLeagueLive(season, league),
                        league.getLeagueCode().name()
                );
                if (result.updated() > 0 || result.finishedDetected() > 0
                        || !result.pendingFullMatchIds().isEmpty()) {
                    log.info("24score LIVE {} updated={} finished={} pendingFull={}",
                            result.leagueCode(), result.updated(), result.finishedDetected(),
                            result.pendingFullMatchIds().size());
                }
                pendingFull.addAll(result.pendingFullMatchIds());
            } catch (RuntimeException e) {
                log.warn("24score LIVE sync failed for {}: {}", league.getLeagueCode(), e.getMessage());
            }
        }

        if (layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH) && !pendingFull.isEmpty()) {
            try {
                matchFinalizeOrchestrator.finalizePendingFullMatches(new ArrayList<>(pendingFull));
            } catch (RuntimeException fullEx) {
                log.warn("FULL after LIVE failed: {}", fullEx.getMessage());
            }
        }
    }

    /**
     * @param afterPoll if true, active matches wait for the poll interval instead of firing immediately
     */
    Optional<Instant> computeNextWake(Instant now, boolean afterPoll) {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.LIVE)) {
            return Optional.empty();
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return Optional.empty();
        }
        Season season = seasonOpt.get();
        boolean hasActiveOrPendingFull = false;
        Instant nearestKickoff = null;

        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null || league.getId() == null) {
                continue;
            }
            if (!TwentyFourScoreLeagueTitles.supported().contains(league.getLeagueCode())) {
                continue;
            }
            for (MatchSchedule schedule : matchScheduleRepository.findByLeagueIdAndSeasonId(
                    league.getId(), season.getId())) {
                if (TwentyFourScoreLiveSupport.isLiveHttpCandidate(schedule, now)
                        || TwentyFourScoreLiveSupport.needsFullMatch(schedule)) {
                    hasActiveOrPendingFull = true;
                }
                Instant kickoff = TwentyFourScoreLiveSupport.upcomingKickoffOrNull(schedule, now);
                if (kickoff != null && (nearestKickoff == null || kickoff.isBefore(nearestKickoff))) {
                    nearestKickoff = kickoff;
                }
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
