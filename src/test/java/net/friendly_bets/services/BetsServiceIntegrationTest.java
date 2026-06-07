package net.friendly_bets.services;

import net.friendly_bets.dto.BetDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.IntegrationTestDtoFactory;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.friendly_bets.support.TestDataFactory.defaultLossGameScore;
import static net.friendly_bets.support.TestDataFactory.defaultWinGameScore;
import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.*;

class BetsServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    BetsService betsService;

    @Autowired
    TestDataFactory testData;

    @Autowired
    BetsRepository betsRepository;

    @Autowired
    PlayerStatsRepository playerStatsRepository;

    @Autowired
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;

    // ------------------------------------------------------------------------------------------------------ //
    // addOpenedBet / setBetResult
    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("addOpenedBet should increase player totalBets only")
    void addOpenedBet_shouldIncreaseTotalBetsOnly() {
        TestFixture fx = testData.createMinimalSeasonSetup();
        testData.addOpenedBetViaService(fx);

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        PlayerStats totalStats = getTotalPlayerStats(fx);

        assertEquals(1, leagueStats.getTotalBets());
        assertEquals(0, leagueStats.getBetCount());
        assertEquals(0.0, leagueStats.getActualBalance());

        assertEquals(1, totalStats.getTotalBets());
        assertEquals(0, totalStats.getBetCount());
    }

    @Test
    @DisplayName("setBetResult should update player and team stats for WON bet")
    void setBetResult_shouldUpdateStatsForWonBet() {
        TestFixture fx = testData.createSeasonWithWonBet();

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        assertEquals(1, leagueStats.getTotalBets());
        assertEquals(1, leagueStats.getBetCount());
        assertEquals(1, leagueStats.getWonBetCount());
        assertEquals(10.0, leagueStats.getActualBalance());

        assertTrue(playerStatsByTeamsRepository
                .findBySeasonIdAndLeagueIdAndUserId(fx.getSeason().getId(), fx.getLeague().getId(), fx.getPlayer().getId())
                .isPresent());
    }

    @Test
    @DisplayName("setBetResult should reject already processed bet")
    void setBetResult_shouldThrowWhenBetAlreadyProcessed() {
        TestFixture fx = testData.createSeasonWithWonBet();

        ConflictException ex = assertThrows(ConflictException.class,
                () -> testData.setBetResultViaService(fx, Bet.BetStatus.LOST, defaultLossGameScore()));

        assertEquals("betAlreadyProcessed", ex.getMessage());
    }

    // ------------------------------------------------------------------------------------------------------ //
    // editBet
    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("editBet should reject OPENED -> WON status transition")
    void editBet_shouldRejectOpenedToWon() {
        TestFixture fx = testData.createSeasonWithOpenedBet();
        Bet bet = fx.getBet();

        var editedBet = IntegrationTestDtoFactory.editedBetFrom(
                fx, bet, Bet.BetStatus.WON, 2.0, 10, defaultWinGameScore());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> betsService.editBet(fx.getModerator().getId(), bet.getId(), editedBet));

        assertEquals("betStatusTransitionNotAllowed", ex.getMessage());
        assertEquals(Bet.BetStatus.OPENED, betsRepository.findById(bet.getId()).orElseThrow().getBetStatus());
    }

    @Test
    @DisplayName("editBet should reject WON -> OPENED status transition")
    void editBet_shouldRejectWonToOpened() {
        TestFixture fx = testData.createSeasonWithWonBet();
        Bet bet = fx.getBet();

        var editedBet = IntegrationTestDtoFactory.editedBetFrom(
                fx, bet, Bet.BetStatus.OPENED, 2.0, 10, null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> betsService.editBet(fx.getModerator().getId(), bet.getId(), editedBet));

        assertEquals("betStatusTransitionNotAllowed", ex.getMessage());
    }

    @Test
    @DisplayName("editBet should allow WON -> LOST and update balance and player stats")
    void editBet_shouldAllowWonToLostAndUpdateStats() {
        TestFixture fx = testData.createSeasonWithWonBet();
        Bet bet = fx.getBet();

        var editedBet = IntegrationTestDtoFactory.editedBetFrom(
                fx, bet, Bet.BetStatus.LOST, 2.0, 10, defaultLossGameScore());

        BetDto result = betsService.editBet(fx.getModerator().getId(), bet.getId(), editedBet);

        assertEquals(Bet.BetStatus.LOST.name(), result.getBetStatus());
        assertEquals(-10.0, result.getBalanceChange());

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        assertEquals(1, leagueStats.getBetCount());
        assertEquals(0, leagueStats.getWonBetCount());
        assertEquals(1, leagueStats.getLostBetCount());
        assertEquals(-10.0, leagueStats.getActualBalance());
    }

    @Test
    @DisplayName("editBet should allow OPENED edit without changing betCount in player stats")
    void editBet_shouldAllowOpenedEditWithoutChangingBetCount() {
        TestFixture fx = testData.createSeasonWithOpenedBet();
        Bet bet = fx.getBet();

        var editedBet = IntegrationTestDtoFactory.editedBetFrom(
                fx, bet, Bet.BetStatus.OPENED, 3.0, 15, null);

        betsService.editBet(fx.getModerator().getId(), bet.getId(), editedBet);

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        assertEquals(1, leagueStats.getTotalBets());
        assertEquals(0, leagueStats.getBetCount());
        assertEquals(0.0, leagueStats.getActualBalance());

        Bet updated = betsRepository.findById(bet.getId()).orElseThrow();
        assertEquals(3.0, updated.getBetOdds());
        assertEquals(15, updated.getBetSize());
    }

    // ------------------------------------------------------------------------------------------------------ //
    // deleteBet
    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("deleteBet should revert totalBets when deleting OPENED bet")
    void deleteBet_shouldRevertTotalBetsWhenDeletingOpenedBet() {
        TestFixture fx = testData.createSeasonWithOpenedBet();
        Bet bet = fx.getBet();

        betsService.deleteBet(
                fx.getModerator().getId(),
                bet.getId(),
                IntegrationTestDtoFactory.deletedBetFrom(fx));

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        assertEquals(0, leagueStats.getTotalBets());
        assertEquals(Bet.BetStatus.DELETED, betsRepository.findById(bet.getId()).orElseThrow().getBetStatus());
    }

    @Test
    @DisplayName("deleteBet should revert completed stats when deleting WON bet")
    void deleteBet_shouldRevertStatsWhenDeletingWonBet() {
        TestFixture fx = testData.createSeasonWithWonBet();
        Bet bet = fx.getBet();

        betsService.deleteBet(
                fx.getModerator().getId(),
                bet.getId(),
                IntegrationTestDtoFactory.deletedBetFrom(fx));

        PlayerStats leagueStats = getLeaguePlayerStats(fx);
        assertEquals(0, leagueStats.getTotalBets());
        assertEquals(0, leagueStats.getBetCount());
        assertEquals(0.0, leagueStats.getActualBalance());

        assertTrue(playerStatsByTeamsRepository
                .findBySeasonIdAndLeagueIdAndUserId(fx.getSeason().getId(), fx.getLeague().getId(), fx.getPlayer().getId())
                .isEmpty());
    }

    // ------------------------------------------------------------------------------------------------------ //

    private PlayerStats getLeaguePlayerStats(TestFixture fx) {
        return playerStatsRepository
                .findBySeasonIdAndLeagueIdAndUser(fx.getSeason().getId(), fx.getLeague().getId(), fx.getPlayer())
                .orElseThrow();
    }

    private PlayerStats getTotalPlayerStats(TestFixture fx) {
        return playerStatsRepository
                .findBySeasonIdAndLeagueIdAndUser(fx.getSeason().getId(), TOTAL_ID, fx.getPlayer())
                .orElseThrow();
    }
}
