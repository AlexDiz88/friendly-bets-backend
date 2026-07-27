package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.MatchdaySettleResultDto;
import net.friendly_bets.dto.SettleMatchdayFromGameResultsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.GameScoreValidator;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Settles OPENED bets from finalized {@link MatchSchedule} rows (replaces game_results settle).
 */
@Service
@RequiredArgsConstructor
public class MatchScheduleSettleService {

    private final MatchScheduleRepository matchScheduleRepository;
    private final GetEntityService getEntityService;
    private final BetsService betsService;
    private final StatsService statsService;

    @Transactional
    public MatchdaySettleResultDto settleMatchdayAndRecalculateStats(
            String moderatorId,
            SettleMatchdayFromGameResultsDto request
    ) {
        Season season = getEntityService.getSeasonOrThrow(request.getSeasonId());
        List<MatchSchedule> schedules = matchScheduleRepository
                .findByLeagueCodeAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(
                        request.getLeagueCode(),
                        season.getId(),
                        request.getMatchday()
                );

        List<GameResult> gameResults = new ArrayList<>();
        for (MatchSchedule schedule : schedules) {
            if (!isFinalizedForSettle(schedule)) {
                continue;
            }
            if (schedule.getLeagueId() == null || schedule.getLeagueId().isBlank()) {
                continue;
            }
            gameResults.add(GameResult.builder()
                    .leagueId(schedule.getLeagueId())
                    .homeTeamId(schedule.getHomeTeamId())
                    .awayTeamId(schedule.getAwayTeamId())
                    .gameScore(schedule.getGameScore())
                    .build());
        }

        if (gameResults.isEmpty()) {
            throw new BadRequestException("noFinalizedGameResultsForMatchday");
        }

        BetsPage betsPage = betsService.setBetResults(moderatorId, season.getId(), gameResults, false);
        Set<String> affectedCalendarNodeIds = collectCalendarNodeIds(betsPage);
        int gameweeksRecalculated = statsService.recalculateGameweekStatsFromEarliest(
                season.getId(),
                affectedCalendarNodeIds
        );

        int betsProcessed = betsPage.getBets() != null ? betsPage.getBets().size() : 0;
        return MatchdaySettleResultDto.builder()
                .matchesSubmitted(gameResults.size())
                .betsProcessed(betsProcessed)
                .gameweekStatsRecalculated(gameweeksRecalculated > 0)
                .build();
    }

    public static boolean isFinalizedForSettle(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getFinalizedAt() == null) {
            return false;
        }
        return GameScoreValidator.hasValidFullTime(schedule.getGameScore());
    }

    private static Set<String> collectCalendarNodeIds(BetsPage betsPage) {
        Set<String> ids = new LinkedHashSet<>();
        if (betsPage == null || betsPage.getBets() == null) {
            return ids;
        }
        for (BetDto bet : betsPage.getBets()) {
            if (bet.getCalendarNodeId() != null && !bet.getCalendarNodeId().isBlank()) {
                ids.add(bet.getCalendarNodeId().trim());
            }
        }
        return ids;
    }
}
