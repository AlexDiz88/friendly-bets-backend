package net.friendly_bets.services;

import net.friendly_bets.dto.NewBetDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TwoPlayersTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.friendly_bets.support.IntegrationGameweekAssertions.*;
import static net.friendly_bets.support.IntegrationTestDtoFactory.deletedBetFrom;
import static net.friendly_bets.support.IntegrationTestDtoFactory.editedBetForUser;
import static net.friendly_bets.support.TestDataFactory.defaultLossGameScore;
import static net.friendly_bets.support.TestDataFactory.defaultWinGameScore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Gameweek: подведение итогов, лимиты ставок на игрока, откат итогов при удалении ставки.
 */
class GameweekStatsIntegrationTest extends AbstractMongoIntegrationTest {

    private static final double ODDS = 2.0;
    private static final int SIZE = 10;
    private static final double WON_BALANCE = ODDS * SIZE - SIZE;
    private static final double LOST_BALANCE = -SIZE;

    @Autowired
    BetsService betsService;

    @Autowired
    TestDataFactory testData;

    @Autowired
    BetsRepository betsRepository;

    @Autowired
    CalendarsRepository calendarsRepository;

    @Nested
    @DisplayName("gameweek settlement")
    class GameweekSettlement {

        private TwoPlayersTestFixture fixture;
        private Bet playerOneBet;
        private Bet playerTwoBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(1);
            playerOneBet = testData.addOpenedBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
            playerTwoBet = testData.addOpenedBet(
                    fixture, fixture.getPlayerTwo(), fixture.getAwayTeam(), fixture.getHomeTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should stay open until all required bets are completed")
        void shouldFinishOnlyAfterAllBetsCompleted() {
            String calendarId = fixture.getCalendarNode().getId();

            assertGameweekOpen(reloadCalendar(calendarsRepository, calendarId));

            betsService.setBetResult(
                    fixture.getModerator().getId(),
                    playerOneBet.getId(),
                    net.friendly_bets.models.BetResult.builder()
                            .betStatus(Bet.BetStatus.WON.name())
                            .gameScore(defaultWinGameScore())
                            .build());

            assertGameweekOpen(reloadCalendar(calendarsRepository, calendarId));

            betsService.setBetResult(
                    fixture.getModerator().getId(),
                    playerTwoBet.getId(),
                    net.friendly_bets.models.BetResult.builder()
                            .betStatus(Bet.BetStatus.LOST.name())
                            .gameScore(defaultLossGameScore())
                            .build());

            CalendarNode finished = reloadCalendar(calendarsRepository, calendarId);
            assertGameweekFinished(finished, 2);
            assertGameweekBalance(finished, fixture.getPlayerOne().getId(), WON_BALANCE, WON_BALANCE);
            assertGameweekBalance(finished, fixture.getPlayerTwo().getId(), LOST_BALANCE, LOST_BALANCE);
            assertEquals(1, findGameweekStats(finished, fixture.getPlayerOne().getId()).getPositionAfterGameweek());
            assertEquals(2, findGameweekStats(finished, fixture.getPlayerTwo().getId()).getPositionAfterGameweek());
        }
    }

    @Nested
    @DisplayName("bet limit per player")
    class BetLimitPerPlayer {

        private TwoPlayersTestFixture fixture;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(1);
        }

        @Test
        @DisplayName("addOpenedBet should reject when player exceeds matchday limit")
        void addOpenedBet_shouldRejectSecondBetFromSamePlayer() {
            testData.addOpenedBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);

            // Другая пара команд (away vs home), иначе сработает betAlreadyAdded, а не лимит тура
            NewBetDto secondBet = NewBetDto.builder()
                    .userId(fixture.getPlayerOne().getId())
                    .seasonId(fixture.getSeason().getId())
                    .leagueId(fixture.getLeague().getId())
                    .matchDay(fixture.getMatchDay())
                    .homeTeamId(fixture.getAwayTeam().getId())
                    .awayTeamId(fixture.getHomeTeam().getId())
                    .betTitle(testData.createDefaultBetTitle())
                    .betOdds(ODDS)
                    .betSize(SIZE)
                    .calendarNodeId(fixture.getCalendarNode().getId())
                    .build();

            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> betsService.addOpenedBet(fixture.getModerator().getId(), secondBet)
            );
            assertEquals("exceededLimitBetsFromPlayer", ex.getMessage());
        }

        @Test
        @DisplayName("editBet should reject reassignment to player who already reached limit")
        void editBet_shouldRejectUserChangeWhenNewPlayerAtLimit() {
            Bet playerOneBet = testData.addOpenedBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
            testData.addOpenedBet(
                    fixture, fixture.getPlayerTwo(), fixture.getAwayTeam(), fixture.getHomeTeam(), ODDS, SIZE);

            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> betsService.editBet(
                            fixture.getModerator().getId(),
                            playerOneBet.getId(),
                            editedBetForUser(fixture, playerOneBet, fixture.getPlayerTwo())
                    )
            );
            assertEquals("exceededLimitBetsFromPlayer", ex.getMessage());
        }

        @Test
        @DisplayName("editBet should allow user change when new player has free slot")
        void editBet_shouldAllowUserChangeWhenNewPlayerUnderLimit() {
            Bet playerOneBet = testData.addOpenedBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);

            betsService.editBet(
                    fixture.getModerator().getId(),
                    playerOneBet.getId(),
                    editedBetForUser(fixture, playerOneBet, fixture.getPlayerTwo())
            );

            Bet updated = betsRepository.findById(playerOneBet.getId()).orElseThrow();
            assertEquals(fixture.getPlayerTwo().getId(), updated.getUser().getId());
        }
    }

    @Nested
    @DisplayName("revert gameweek settlement on delete")
    class RevertGameweekOnDelete {

        private TwoPlayersTestFixture fixture;
        private Bet playerTwoBet;

        @BeforeEach
        void seedCompletedGameweek() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(1);
            testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
            playerTwoBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerTwo(), fixture.getAwayTeam(), fixture.getHomeTeam(), ODDS, SIZE);

            assertGameweekFinished(reloadCalendar(calendarsRepository, fixture.getCalendarNode().getId()), 2);
        }

        @Test
        @DisplayName("deleteBet should reopen gameweek and clear gameweek stats")
        void deleteBet_shouldReopenGameweekAndClearStats() {
            betsService.deleteBet(
                    fixture.getModerator().getId(),
                    playerTwoBet.getId(),
                    deletedBetFrom(fixture)
            );

            CalendarNode calendarNode = reloadCalendar(calendarsRepository, fixture.getCalendarNode().getId());
            assertGameweekOpen(calendarNode);

            Bet deleted = betsRepository.findById(playerTwoBet.getId()).orElseThrow();
            assertEquals(Bet.BetStatus.DELETED, deleted.getBetStatus());
        }

        @Test
        @DisplayName("deleteBet then re-complete remaining bets should settle gameweek again")
        void deleteBet_thenCompleteAgain_shouldSettleGameweek() {
            betsService.deleteBet(
                    fixture.getModerator().getId(),
                    playerTwoBet.getId(),
                    deletedBetFrom(fixture)
            );

            assertGameweekOpen(reloadCalendar(calendarsRepository, fixture.getCalendarNode().getId()));

            Bet replacement = testData.addOpenedBet(
                    fixture, fixture.getPlayerTwo(), fixture.getAwayTeam(), fixture.getHomeTeam(), ODDS, SIZE);
            betsService.setBetResult(
                    fixture.getModerator().getId(),
                    replacement.getId(),
                    net.friendly_bets.models.BetResult.builder()
                            .betStatus(Bet.BetStatus.LOST.name())
                            .gameScore(defaultLossGameScore())
                            .build());

            assertGameweekFinished(reloadCalendar(calendarsRepository, fixture.getCalendarNode().getId()), 2);
        }
    }
}
