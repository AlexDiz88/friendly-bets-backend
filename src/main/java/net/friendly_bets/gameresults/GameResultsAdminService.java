package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.AdminCorrectGameResultDto;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.MatchdaySettleResultDto;
import net.friendly_bets.dto.SettleMatchdayFromGameResultsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import net.friendly_bets.gameresults.GameResultDisplayService;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.Season;
import net.friendly_bets.gameresults.CanonicalScoreNormalizer;
import net.friendly_bets.gameresults.GameScoreConsistencyValidator;
import net.friendly_bets.models.gameresults.GameResultFinalizedSource;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.BetsService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.StatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameResultsAdminService {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultDisplayService gameResultDisplayService;
    private final GetEntityService getEntityService;
    private final BetsService betsService;
    private final StatsService statsService;
    private final MatchdaySlotSupport matchdaySupport;
    private final CanonicalScoreNormalizer canonicalScoreNormalizer;

    @Transactional
    public ExternalMatchDto applyPrimaryApiScore(String gameResultId) {
        GameResultRecord record = gameResultRecordRepository.findById(gameResultId)
                .orElseThrow(() -> new NotFoundException("GameResult", gameResultId));

        GameResultSourceSnapshot source = record.primaryExternalSource();
        if (source == null || source.getGameScore() == null) {
            throw new BadRequestException("noPrimaryApiScoreSnapshot");
        }

        GameScore normalized = canonicalScoreNormalizer.normalizeFromRawSnapshot(
                source.getGameScore(),
                source.getScoreDuration() != null ? source.getScoreDuration() : record.getScoreDuration()
        );
        if (normalized == null || !GameScoreConsistencyValidator.isConsistent(normalized)) {
            throw new BadRequestException("incorrectGameScore");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setGameScore(normalized);
        record.setScoreDuration(source.getScoreDuration());
        record.setStatus("FINISHED");
        record.setFetchedAt(now);
        record.setFinalizedAt(now);
        record.setFinalizedSource(GameResultFinalizedSource.API.name());
        record.setAdminCorrected(false);
        record.setStableScorePollCount(Math.max(record.getStableScorePollCount(), 2));
        record.setLastSeenCanonicalScoreHash(
                net.friendly_bets.gameresults.MatchResultStabilizationService.canonicalScoreHash(normalized));

        gameResultRecordRepository.save(record);
        return gameResultDisplayService.toDisplayDto(record);
    }

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
        if (!GameScoreConsistencyValidator.isConsistent(score)) {
            throw new BadRequestException("incorrectGameScore");
        }

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
            externalSeason = matchdaySupport.resolveExternalSeasonYear(season);
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

        // Gameweek stats — не на каждую ставку; частичный пересчёт с затронутых calendar node до конца сезона.
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

    private static String trimOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
