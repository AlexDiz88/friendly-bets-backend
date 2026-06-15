package net.friendly_bets.services;

import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TwoPlayersTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static net.friendly_bets.support.TestDataFactory.authUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAddOpenedBetIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    BetsService betsService;

    @Autowired
    TestDataFactory testData;

    @Autowired
    GameResultRecordRepository gameResultRecordRepository;

    @Autowired
    SeasonsRepository seasonsRepository;

    @Test
    @DisplayName("USER addOpenedBet succeeds before match kickoff when game result exists")
    void userAddOpenedBet_beforeKickoff_succeeds() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        saveScheduledMatch(fx, GameResultNotStarted.nowUtc().plusHours(3));

        NewBetDto bet = newBetForPlayer(fx);
        var result = betsService.addOpenedBet(authUser(fx.getPlayerOne()), bet);
        assertNotNull(result.getId());
    }

    @Test
    @DisplayName("USER addOpenedBet rejects after match kickoff")
    void userAddOpenedBet_afterKickoff_rejects() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        saveScheduledMatch(fx, GameResultNotStarted.nowUtc().minusMinutes(5));

        NewBetDto bet = newBetForPlayer(fx);
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> betsService.addOpenedBet(authUser(fx.getPlayerOne()), bet)
        );
        assertEquals("matchAlreadyStarted", ex.getMessage());
    }

    @Test
    @DisplayName("MODERATOR addOpenedBet allowed after match kickoff")
    void moderatorAddOpenedBet_afterKickoff_succeeds() {
        TwoPlayersTestFixture fx = testData.createTwoPlayersFirstMatchdaySetup(2);
        setSeasonDates(fx.getSeason());
        saveScheduledMatch(fx, GameResultNotStarted.nowUtc().minusMinutes(5));

        NewBetDto bet = newBetForPlayer(fx);
        var result = betsService.addOpenedBet(authUser(fx.getModerator()), bet);
        assertNotNull(result.getId());
    }

    private void setSeasonDates(Season season) {
        season.setStartDate(LocalDate.of(2025, 8, 1));
        season.setEndDate(LocalDate.of(2026, 5, 31));
        seasonsRepository.save(season);
    }

    private void saveScheduledMatch(TwoPlayersTestFixture fx, LocalDateTime kickoff) {
        gameResultRecordRepository.save(GameResultRecord.builder()
                .leagueCode("EPL")
                .matchday(1)
                .season("2025")
                .status("SCHEDULED")
                .utcDate(kickoff)
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .leagueId(fx.getLeague().getId())
                .fetchedAt(LocalDateTime.now())
                .provider(MatchDataProviders.FOURSCORE)
                .sources(Map.of(
                        MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE),
                        GameResultSourceSnapshot.builder()
                                .externalMatchId(42_001L)
                                .externalCompetitionCode("PL")
                                .home(GameResultSideSnapshot.builder().externalId("1").externalName("Home").build())
                                .away(GameResultSideSnapshot.builder().externalId("2").externalName("Away").build())
                                .build()
                ))
                .build());
    }

    private static NewBetDto newBetForPlayer(TwoPlayersTestFixture fx) {
        return NewBetDto.builder()
                .userId(fx.getPlayerOne().getId())
                .seasonId(fx.getSeason().getId())
                .leagueId(fx.getLeague().getId())
                .matchDay(fx.getMatchDay())
                .homeTeamId(fx.getHomeTeam().getId())
                .awayTeamId(fx.getAwayTeam().getId())
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HOME_WIN.getCode())
                        .label(BetTitleCode.HOME_WIN.getLabel())
                        .isNot(false)
                        .build())
                .betOdds(2.0)
                .betSize(10)
                .calendarNodeId(fx.getCalendarNode().getId())
                .build();
    }
}
