package net.friendly_bets.footballdata;

import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.*;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.ExternalMatchRepository;
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

import static org.junit.jupiter.api.Assertions.*;

class AutoBetSettlementIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    AutoBetSettlementService autoBetSettlementService;

    @Autowired
    ExternalMatchRepository externalMatchRepository;

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
    @DisplayName("settleSeason processes OPENED bets when external match is FINISHED")
    void settleSeason_closesMatchingOpenedBets() {
        TestFixture fx = testData.createSeasonWithOpenedBet();
        footballDataProperties.setSystemModeratorId(fx.getModerator().getId());
        Season season = fx.getSeason();
        season.setStartDate(LocalDate.of(2025, 8, 1));
        seasonsRepository.save(season);

        externalMatchRepository.save(ExternalMatch.builder()
                .externalMatchId(99_001L)
                .competitionCode("PL")
                .matchday(1)
                .season("2025")
                .status("FINISHED")
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .leagueId(fx.getLeague().getId())
                .homeTeamName("Home")
                .awayTeamName("Away")
                .gameScore(GameScore.builder().fullTime("2:0").firstTime("1:0").build())
                .fetchedAt(LocalDateTime.now())
                .build());

        AutoSettleResult result = autoBetSettlementService.settleSeason(season);

        assertTrue(result.isExecuted());
        assertEquals(1, result.getMatchesSubmitted());
        assertTrue(result.getBetsProcessed() >= 1);

        Bet updated = betsRepository.findById(fx.getBet().getId()).orElseThrow();
        assertNotEquals(Bet.BetStatus.OPENED, updated.getBetStatus());
        assertNotNull(updated.getGameScore());
        assertEquals("2:0", updated.getGameScore().getFullTime());
    }
}
