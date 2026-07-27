package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.gameresults.GameScoreValidator;
import net.friendly_bets.gameresults.config.MatchResultSyncProperties;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.User;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * After LIVE detects finished: FULL_MATCH (primary→secondary) then optional bet settle.
 */
@Service
@RequiredArgsConstructor
public class MatchFinalizeOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MatchFinalizeOrchestrator.class);

    private final LayerProviderRouter router;
    private final MatchScheduleRepository matchScheduleRepository;
    private final BetsService betsService;
    private final StatsService statsService;
    private final MatchResultSyncProperties properties;
    private final UsersRepository usersRepository;
    private final ExternalDataLayerConfigService layerConfigService;
    private final ExternalApiMonitoringService monitoringService;

    @Transactional
    public MatchSchedule finalizeFinishedMatch(MatchSchedule match) {
        if (match == null || match.getId() == null) {
            return match;
        }
        MatchSchedule current = matchScheduleRepository.findById(match.getId()).orElse(match);
        if (current.getFullDetailsFetchedAt() == null
                && layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH)) {
            final MatchSchedule toFetch = current;
            current = router.execute(
                    ExternalDataLayer.FULL_MATCH,
                    FullMatchProvider.class,
                    p -> p.fetchAndPersistFullDetails(toFetch)
            );
        }
        if (properties.isAutoSettleEnabled() && MatchScheduleSettleService.isFinalizedForSettle(current)) {
            settleMatch(current);
        }
        return current;
    }

    /** Called by LIVE scheduler/admin after sync — keeps LiveProvider free of Router/Config cycle. */
    public void finalizePendingFullMatches(List<String> matchScheduleIds) {
        if (matchScheduleIds == null || matchScheduleIds.isEmpty()) {
            return;
        }
        List<MatchSchedule> withKickoff = new ArrayList<>();
        int missingKickoff = 0;
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
            withKickoff.add(loaded);
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
        for (MatchSchedule match : withKickoff) {
            try {
                finalizeFinishedMatch(match);
            } catch (RuntimeException e) {
                // LayerProviderRouter already wrote error_logs for FULL failure.
                log.warn("FULL/settle failed for match {}: {}", match.getId(), e.getMessage());
            }
        }
    }

    private void settleMatch(MatchSchedule schedule) {
        if (!GameScoreValidator.hasValidFullTime(schedule.getGameScore())) {
            return;
        }
        String moderatorId = resolveSystemModeratorId();
        if (moderatorId == null) {
            log.warn("Skip auto-settle: no ADMIN user for match {}", schedule.getId());
            return;
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
