package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.FullMatchSyncResultDto;
import net.friendly_bets.exceptions.FullMatchNotReadyException;
import net.friendly_bets.matchschedule.GameScoreValidator;
import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.User;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchAttemptSupport;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.live.LiveMatchSupport;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * After LIVE detects finished: wait initial delay, FULL_MATCH (primary→secondary),
 * require provider FINISHED status, then optional bet settle.
 * Unsuccessful FULL (not-ready / not-found / parse / HTTP) → defer 5 min, then hourly.
 */
@Service
@RequiredArgsConstructor
public class MatchFinalizeOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MatchFinalizeOrchestrator.class);
    private static final List<Bet.BetStatus> OPENED_ONLY = List.of(Bet.BetStatus.OPENED);

    private final LayerProviderRouter router;
    private final MatchScheduleRepository matchScheduleRepository;
    private final BetsRepository betsRepository;
    private final BetsService betsService;
    private final StatsService statsService;
    private final MatchResultSyncProperties properties;
    private final UsersRepository usersRepository;
    private final ExternalDataLayerConfigService layerConfigService;
    private final ExternalApiMonitoringService monitoringService;
    private final ErrorLogService errorLogService;
    private final StandingsSyncOrchestrator standingsSyncOrchestrator;

    /** Spring proxy — required so {@code @Transactional} on finalize applies from wake/admin loops. */
    private MatchFinalizeOrchestrator self;

    @Autowired
    public void setSelf(@Lazy MatchFinalizeOrchestrator self) {
        this.self = self;
    }

    @Transactional
    public MatchSchedule finalizeFinishedMatch(MatchSchedule match) {
        if (match == null || match.getId() == null) {
            return match;
        }
        MatchSchedule current = matchScheduleRepository.findById(match.getId()).orElse(match);
        boolean wasFinalized = current.getFinalizedAt() != null;
        Instant now = Instant.now();
        if (current.getFullDetailsFetchedAt() == null
                && layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH)) {
            if (!FullMatchAttemptSupport.isAttemptDue(current, now, properties)) {
                return current;
            }
            final MatchSchedule toFetch = current;
            try {
                current = router.execute(
                        ExternalDataLayer.FULL_MATCH,
                        FullMatchProvider.class,
                        p -> p.fetchAndPersistFullDetails(toFetch)
                );
                if (current.getFullDetailsFetchedAt() != null) {
                    FullMatchAttemptSupport.clearAttemptState(current);
                    current = matchScheduleRepository.save(current);
                }
            } catch (FullMatchNotReadyException e) {
                return handleUnsuccessfulAttempt(current, now, e);
            } catch (RuntimeException e) {
                log.warn("FULL failed for match {}: {}", current.getId(), e.getMessage());
                return handleUnsuccessfulAttempt(current, now, null);
            }
        }
        tryAutoSettle(current);
        if (!wasFinalized && MatchScheduleSettleService.isFinalizedForSettle(current)) {
            standingsSyncOrchestrator.syncLeagueAfterMatchFinalized(
                    current.getLeagueCode(),
                    current.getSeasonId()
            );
        }
        return current;
    }

    /**
     * Admin manual FULL_MATCH: selected provider (no failover), UTC kickoff date filter,
     * bypasses initial/retry delay. Settles when auto-settle is enabled.
     */
    public FullMatchSyncResultDto syncByProviderAndUtcDate(
            FullMatchProvider provider,
            Season season,
            LocalDate utcDate
    ) {
        if (provider == null || season == null || season.getId() == null || utcDate == null) {
            return FullMatchSyncResultDto.builder()
                    .provider(provider != null ? provider.providerId() : null)
                    .date(utcDate != null ? utcDate.toString() : null)
                    .build();
        }
        List<MatchSchedule> candidates = matchScheduleRepository.findBySeasonId(season.getId()).stream()
                .filter(LiveMatchSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() != null)
                .filter(s -> LocalDate.ofInstant(s.getUtcKickoff(), ZoneOffset.UTC).equals(utcDate))
                .toList();
        int succeeded = 0;
        int notReady = 0;
        int failed = 0;
        MatchFinalizeOrchestrator target = proxy();
        for (MatchSchedule match : candidates) {
            try {
                MatchSchedule updated = target.finalizeFinishedMatchWithProvider(match, provider);
                if (updated != null && updated.getFullDetailsFetchedAt() != null) {
                    succeeded++;
                } else {
                    // Provider returned without FULL (e.g. alreadyFetched race) — treat as not ready.
                    notReady++;
                }
            } catch (FullMatchNotReadyException e) {
                notReady++;
                log.debug("FULL not ready for match {}: {}", match.getId(), e.getProviderStatus());
            } catch (RuntimeException e) {
                failed++;
                log.warn("FULL/settle failed for match {}: {}", match.getId(), e.getMessage());
            }
        }
        return FullMatchSyncResultDto.builder()
                .provider(provider.providerId())
                .date(utcDate.toString())
                .candidates(candidates.size())
                .succeeded(succeeded)
                .notReady(notReady)
                .failed(failed)
                .build();
    }

    /**
     * Admin repair: clear FULL card / finalize stamps so the match is FULL-eligible again,
     * then run {@link #finalizeFinishedMatchWithProvider} (FULL + auto-settle).
     */
    @Transactional
    public MatchSchedule resetFullCardAndRerun(MatchSchedule match, FullMatchProvider provider) {
        if (match == null || match.getId() == null || provider == null) {
            return match;
        }
        MatchSchedule current = matchScheduleRepository.findById(match.getId()).orElse(match);
        clearFullCardState(current);
        if (current.getStatus() == null || current.getStatus().isBlank()) {
            current.setStatus("FINISHED");
        }
        current = matchScheduleRepository.save(current);
        return finalizeFinishedMatchWithProvider(current, provider);
    }

    static void clearFullCardState(MatchSchedule schedule) {
        if (schedule == null) {
            return;
        }
        schedule.setFullDetailsFetchedAt(null);
        schedule.setFinalizedAt(null);
        schedule.setFinalizedByProvider(null);
        schedule.setGameScore(null);
        schedule.setGoals(new ArrayList<>());
        schedule.setStats(null);
        schedule.setAddedTimeFirstHalf(null);
        schedule.setAddedTimeSecondHalf(null);
        schedule.setFullMatchNextAttemptAt(null);
        schedule.setFullMatchNotReadyCount(null);
        schedule.setSettleFailCount(null);
        schedule.setSettleNextAttemptAt(null);
    }

    /**
     * Same as {@link #finalizeFinishedMatch} but uses a concrete provider (admin manual)
     * and skips attempt-due / layer-enabled gates.
     */
    @Transactional
    public MatchSchedule finalizeFinishedMatchWithProvider(MatchSchedule match, FullMatchProvider provider) {
        if (match == null || match.getId() == null || provider == null) {
            return match;
        }
        MatchSchedule current = matchScheduleRepository.findById(match.getId()).orElse(match);
        boolean wasFinalized = current.getFinalizedAt() != null;
        Instant now = Instant.now();
        if (current.getFullDetailsFetchedAt() == null) {
            try {
                current = provider.fetchAndPersistFullDetails(current);
                if (current.getFullDetailsFetchedAt() != null) {
                    FullMatchAttemptSupport.clearAttemptState(current);
                    current = matchScheduleRepository.save(current);
                }
            } catch (FullMatchNotReadyException e) {
                handleUnsuccessfulAttempt(current, now, e);
                throw e;
            } catch (RuntimeException e) {
                log.warn("FULL failed for match {}: {}", current.getId(), e.getMessage());
                handleUnsuccessfulAttempt(current, now, null);
                throw e;
            }
        }
        tryAutoSettle(current);
        if (!wasFinalized && MatchScheduleSettleService.isFinalizedForSettle(current)) {
            standingsSyncOrchestrator.syncLeagueAfterMatchFinalized(
                    current.getLeagueCode(),
                    current.getSeasonId()
            );
        }
        return current;
    }

    /** Called by LIVE scheduler/admin after sync — keeps LiveProvider free of Router/Config cycle. */
    public void finalizePendingFullMatches(List<String> matchScheduleIds) {
        if (matchScheduleIds == null || matchScheduleIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        List<MatchSchedule> due = new ArrayList<>();
        int missingKickoff = 0;
        int deferred = 0;
        String leagueCode = null;
        String seasonId = null;
        for (String id : matchScheduleIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            MatchSchedule loaded = matchScheduleRepository.findById(id.trim()).orElse(null);
            if (loaded == null) {
                continue;
            }
            if (loaded.getUtcKickoff() == null) {
                missingKickoff++;
                if (leagueCode == null) {
                    leagueCode = loaded.getLeagueCode();
                }
                if (seasonId == null) {
                    seasonId = loaded.getSeasonId();
                }
                continue;
            }
            if (!FullMatchAttemptSupport.isAttemptDue(loaded, now, properties)) {
                deferred++;
                continue;
            }
            due.add(loaded);
        }
        if (missingKickoff > 0) {
            String provider = layerConfigService.assignment(ExternalDataLayer.FULL_MATCH).getPrimaryProvider();
            ExternalApiMonitoringRun run = monitoringService.begin(
                    ExternalDataLayer.FULL_MATCH,
                    provider,
                    ExternalApiMonitoringTrigger.ORCHESTRATOR,
                    leagueCode,
                    seasonId
            );
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder()
                            .requested(missingKickoff)
                            .skipped(missingKickoff)
                            .saved(0)
                            .build(),
                    List.of(),
                    List.of(),
                    ExternalApiMonitoringService.reasonMissingUtcKickoff(missingKickoff)
            );
        }
        if (deferred > 0) {
            log.debug("FULL deferred (initial/retry delay): {} match(es)", deferred);
        }
        MatchFinalizeOrchestrator target = proxy();
        for (MatchSchedule match : due) {
            try {
                target.finalizeFinishedMatch(match);
            } catch (FullMatchNotReadyException e) {
                log.debug("FULL not ready for match {}: {}", match.getId(), e.getProviderStatus());
            } catch (RuntimeException e) {
                log.warn("FULL/settle failed for match {}: {}", match.getId(), e.getMessage());
            }
        }
    }

    /**
     * Retry settle for finalized matches that still have OPENED bets (FULL already done).
     * Called from LIVE wake so a one-shot settle failure cannot leave permanent orphans.
     * Budget: {@code settle-max-failures} consecutive fails → block {@code settle-block-after-max-ms}.
     */
    public int settleOrphanFinalizedMatches(String seasonId) {
        if (!properties.isAutoSettleEnabled() || seasonId == null || seasonId.isBlank()) {
            return 0;
        }
        Instant now = Instant.now();
        int settled = 0;
        MatchFinalizeOrchestrator target = proxy();
        for (MatchSchedule schedule : matchScheduleRepository.findBySeasonId(seasonId.trim())) {
            if (!MatchScheduleSettleService.isFinalizedForSettle(schedule) || schedule.getId() == null) {
                continue;
            }
            if (!SettleAttemptSupport.isAttemptDue(schedule, now)) {
                continue;
            }
            List<Bet> opened = betsRepository.findAllByMatchScheduleIdAndBetStatusIn(
                    schedule.getId(),
                    OPENED_ONLY
            );
            if (opened == null || opened.isEmpty()) {
                continue;
            }
            try {
                if (target.settleFinalizedMatch(schedule)) {
                    settled++;
                } else {
                    recordSettleFailure(schedule, now);
                }
            } catch (RuntimeException e) {
                log.warn("Orphan settle failed for match {}: {}", schedule.getId(), e.getMessage());
                errorLogService.recordAutoSettleFailure(schedule, e.getMessage());
                recordSettleFailure(schedule, now);
            }
        }
        if (settled > 0) {
            log.info("Orphan settle processed {} finalized match(es) with OPENED bets", settled);
        }
        return settled;
    }

    /**
     * Public settle entry for orphan retry / tests — runs in its own transaction via proxy.
     *
     * @return {@code true} if settle ran (admin found); {@code false} if skipped (e.g. no ADMIN)
     */
    @Transactional
    public boolean settleFinalizedMatch(MatchSchedule schedule) {
        return settleMatch(schedule);
    }

    private void tryAutoSettle(MatchSchedule current) {
        if (!properties.isAutoSettleEnabled() || !MatchScheduleSettleService.isFinalizedForSettle(current)) {
            return;
        }
        Instant now = Instant.now();
        if (!SettleAttemptSupport.isAttemptDue(current, now)) {
            return;
        }
        try {
            if (!settleMatch(current)) {
                recordSettleFailure(current, now);
            }
        } catch (RuntimeException e) {
            log.warn("Auto-settle failed for match {}: {}", current.getId(), e.getMessage());
            errorLogService.recordAutoSettleFailure(current, e.getMessage());
            recordSettleFailure(current, now);
        }
    }

    private void recordSettleFailure(MatchSchedule schedule, Instant now) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        MatchSchedule current = matchScheduleRepository.findById(schedule.getId()).orElse(schedule);
        SettleAttemptSupport.applyFailure(current, now, properties);
        matchScheduleRepository.save(current);
        if (current.getSettleNextAttemptAt() != null) {
            log.warn(
                    "Auto-settle blocked until {} for match {} after {} consecutive failures",
                    current.getSettleNextAttemptAt(),
                    current.getId(),
                    properties.getSettleMaxFailures()
            );
        }
    }

    private MatchSchedule handleUnsuccessfulAttempt(
            MatchSchedule current,
            Instant now,
            FullMatchNotReadyException notReady
    ) {
        Instant nextAt = FullMatchAttemptSupport.nextAttemptAfterFailure(current, now, properties);
        boolean logNotReady = notReady != null && FullMatchAttemptSupport.shouldLogNotReady(current);
        int nextCount = (current.getFullMatchNotReadyCount() != null ? current.getFullMatchNotReadyCount() : 0) + 1;
        current.setFullMatchNotReadyCount(nextCount);
        current.setFullMatchNextAttemptAt(nextAt);
        MatchSchedule saved = matchScheduleRepository.save(current);
        if (logNotReady) {
            String provider = layerConfigService.assignment(ExternalDataLayer.FULL_MATCH).getPrimaryProvider();
            errorLogService.recordFullMatchNotReady(saved, provider, notReady.getProviderStatus());
        }
        log.info("FULL attempt deferred for match {} (count={}, next={})",
                saved.getId(), nextCount, nextAt);
        return saved;
    }

    /**
     * @return {@code true} if settle executed; {@code false} if skipped (not ready / no ADMIN)
     */
    private boolean settleMatch(MatchSchedule schedule) {
        if (!MatchScheduleSettleService.isFinalizedForSettle(schedule)) {
            return false;
        }
        String moderatorId = resolveSystemModeratorId();
        if (moderatorId == null) {
            log.warn("Skip auto-settle: no ADMIN user for match {}", schedule.getId());
            errorLogService.recordAutoSettleSkippedNoAdmin(schedule);
            return false;
        }
        GameResult gameResult = GameResult.builder()
                .leagueId(schedule.getLeagueId())
                .homeTeamId(schedule.getHomeTeamId())
                .awayTeamId(schedule.getAwayTeamId())
                .gameScore(schedule.getGameScore())
                .build();
        BetsPage page = betsService.setBetResults(moderatorId, schedule.getSeasonId(), List.of(gameResult), false);
        if (page.getBets() != null && !page.getBets().isEmpty()) {
            Set<String> nodeIds = page.getBets().stream()
                    .map(b -> b.getCalendarNodeId())
                    .filter(id -> id != null && !id.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            if (!nodeIds.isEmpty()) {
                statsService.recalculateGameweekStatsFromEarliest(schedule.getSeasonId(), nodeIds);
            }
        }
        clearSettleAttemptState(schedule);
        return true;
    }

    private void clearSettleAttemptState(MatchSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        MatchSchedule current = matchScheduleRepository.findById(schedule.getId()).orElse(null);
        if (current == null
                || (current.getSettleFailCount() == null && current.getSettleNextAttemptAt() == null)) {
            return;
        }
        SettleAttemptSupport.clearAttemptState(current);
        matchScheduleRepository.save(current);
    }

    private MatchFinalizeOrchestrator proxy() {
        return self != null ? self : this;
    }

    private String resolveSystemModeratorId() {
        if (properties.getSystemModeratorId() != null && !properties.getSystemModeratorId().isBlank()) {
            return properties.getSystemModeratorId().trim();
        }
        return usersRepository.findFirstByRoleOrderByCreatedAtAsc(User.Role.ADMIN)
                .map(User::getId)
                .orElse(null);
    }
}
