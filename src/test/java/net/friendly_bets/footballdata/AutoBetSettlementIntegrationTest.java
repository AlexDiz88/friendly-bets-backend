package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.*;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AutoBetSettlementIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    AutoBetSettlementService autoBetSettlementService;

    @Autowired
    GameResultRecordRepository gameResultRecordRepository;

    @Autowired
    BetsRepository betsRepository;

    @Autowired
    TestDataFactory testData;

    @Autowired
    SeasonsRepository seasonsRepository;

    @Autowired
    FootballDataProperties footballDataProperties;

    @BeforeEach
    void enableAutoSettle() {
        footballDataProperties.setAutoSettleEnabled(true);
        footballDataProperties.setSystemModeratorId(null);
    }

    @Test
    @DisplayName("settleSeason processes OPENED bets when game result is FINISHED")
    void settleSeason_closesMatchingOpenedBets() {
        TestFixture fx = testData.createSeasonWithOpenedBet();
        footballDataProperties.setSystemModeratorId(fx.getModerator().getId());
        Season season = fx.getSeason();
        season.setStartDate(LocalDate.of(2025, 8, 1));
        seasonsRepository.save(season);

        gameResultRecordRepository.save(GameResultRecord.builder()
                .leagueCode("EPL")
                .matchday(1)
                .season("2025")
                .status("FINISHED")
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .leagueId(fx.getLeague().getId())
                .gameScore(GameScore.builder().fullTime("2:0").firstTime("1:0").build())
                .fetchedAt(LocalDateTime.now())
                .finalizedAt(LocalDateTime.now())
                .finalizedSource("API")
                .provider(MatchDataProviders.FOOTBALL_DATA)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA),
                        GameResultSourceSnapshot.builder()
                                .externalMatchId(99_001L)
                                .externalCompetitionCode("PL")
                                .home(GameResultSideSnapshot.builder().externalId("1").externalName("Home FC").build())
                                .away(GameResultSideSnapshot.builder().externalId("2").externalName("Away FC").build())
                                .build()
                ))
                .build());

        AutoSettleResult result = autoBetSettlementService.settleSeason(season);

        assertTrue(result.isExecuted());
        assertEquals(1, result.getMatchesSubmitted());
        assertTrue(result.getBetsProcessed() >= 1);

        Bet updated = betsRepository.findById(fx.getBet().getId()).orElseThrow();
        assertNotEquals(Bet.BetStatus.OPENED, updated.getBetStatus());
        assertNotNull(updated.getGameScore());
    }
}
