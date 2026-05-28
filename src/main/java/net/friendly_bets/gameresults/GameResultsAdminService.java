package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.AdminCorrectGameResultDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.MatchdaySettleResultDto;
import net.friendly_bets.dto.SettleMatchdayFromGameResultsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.GameResultDisplayService;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultFinalizedSource;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.BetsService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.StatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameResultsAdminService {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultDisplayService gameResultDisplayService;
    private final GetEntityService getEntityService;
    private final BetsService betsService;
    private final StatsService statsService;
    private final FootballDataMatchdaySupport matchdaySupport;

    @Transactional
    public ExternalMatchDto correctScoreByAdmin(String gameResultId, AdminCorrectGameResultDto body) {
        GameResultRecord record = gameResultRecordRepository.findById(gameResultId)
                .orElseThrow(() -> new NotFoundException("GameResult", gameResultId));

        GameScore score = GameScore.builder()
                .fullTime(body.getFullTime().trim())
                .firstTime(trimOrNull(body.getFirstTime()))
                .overTime(trimOrNull(body.getOverTime()))
                .penalty(trimOrNull(body.getPenalty()))
                .build();
        GameScoreValidator.requireValidCanonicalScore(score);

        LocalDateTime now = LocalDateTime.now();
        record.setGameScore(score);
        record.setStatus("FINISHED");
        record.setFetchedAt(now);
        record.setFinalizedAt(now);
        record.setFinalizedSource(GameResultFinalizedSource.ADMIN.name());
        record.setAdminCorrected(true);

        gameResultRecordRepository.save(record);
        return gameResultDisplayService.toDisplayDto(record);
    }

    @Transactional
    public MatchdaySettleResultDto settleMatchdayAndRecalculateStats(
            String moderatorId,
            SettleMatchdayFromGameResultsDto request
    ) {
        Season season = getEntityService.getSeasonOrThrow(request.getSeasonId());
        String externalSeason = request.getExternalSeason();
        if (externalSeason == null || externalSeason.isBlank()) {
            externalSeason = matchdaySupport.resolveFootballDataSeasonYear(season);
        }

        List<GameResultRecord> records = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                request.getLeagueCode(),
                request.getMatchday(),
                externalSeason
        );

        List<GameResult> gameResults = new ArrayList<>();
        for (GameResultRecord record : records) {
            if (!record.isFinalized() || !GameScoreValidator.hasValidFullTime(record.getGameScore())) {
                continue;
            }
            if (record.getLeagueId() == null || record.getLeagueId().isBlank()) {
                continue;
            }
            gameResults.add(GameResult.builder()
                    .leagueId(record.getLeagueId())
                    .homeTeamId(record.getHomeTeamId())
                    .awayTeamId(record.getAwayTeamId())
                    .gameScore(record.getGameScore())
                    .build());
        }

        if (gameResults.isEmpty()) {
            throw new BadRequestException("noFinalizedGameResultsForMatchday");
        }

        BetsPage betsPage = betsService.setBetResults(moderatorId, season.getId(), gameResults);
        statsService.recalculateAllGameweekStats(season.getId());

        int betsProcessed = betsPage.getBets() != null ? betsPage.getBets().size() : 0;
        return MatchdaySettleResultDto.builder()
                .matchesSubmitted(gameResults.size())
                .betsProcessed(betsProcessed)
                .gameweekStatsRecalculated(true)
                .build();
    }

    private static String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
