package net.friendly_bets.services;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.PlayerStatsByBetTitlesRepository;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;
import net.friendly_bets.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.friendly_bets.support.IntegrationStatsAssertions.*;
import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Сценарий: 2 игрока, у первого 2 WON-ставки в 1-м туре, у второго нет ставок.
 * После editBet одна ставка переходит ко второму игроку — проверяем пересчёт всей статистики.
 */
class EditBetUserChangeStatsIntegrationTest extends AbstractMongoIntegrationTest {

    private static final double BET_ONE_ODDS = 2.0;
    private static final int BET_ONE_SIZE = 10;
    private static final double BET_ONE_BALANCE = BET_ONE_ODDS * BET_ONE_SIZE - BET_ONE_SIZE;

    private static final double BET_TWO_ODDS = 3.0;
    private static final int BET_TWO_SIZE = 10;
    private static final double BET_TWO_BALANCE = BET_TWO_ODDS * BET_TWO_SIZE - BET_TWO_SIZE;

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

    @Autowired
    PlayerStatsByBetTitlesRepository playerStatsByBetTitlesRepository;

    private TwoPlayersTestFixture fixture;
    private Bet playerOneBetOne;
    private Bet playerOneBetTwo;

    @BeforeEach
    void seedScenario() {
        fixture = testData.createTwoPlayersFirstMatchdaySetup(2);

        playerOneBetOne = testData.addOpenedAndWonBet(
                fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(),
                BET_ONE_ODDS, BET_ONE_SIZE);

        playerOneBetTwo = testData.addOpenedAndWonBet(
                fixture, fixture.getPlayerOne(), fixture.getAwayTeam(), fixture.getHomeTeam(),
                BET_TWO_ODDS, BET_TWO_SIZE);
    }

    @Test
    @DisplayName("editBet with user change should recalculate player, team and bet-title stats for both users")
    void editBetUserChange_shouldRecalculateAllStats() {
        assertPlayerOneStatsAfterTwoWonBets();
        assertPlayerTwoHasNoStats();

        var editedBet = IntegrationTestDtoFactory.editedBetForUser(fixture, playerOneBetOne, fixture.getPlayerTwo());
        betsService.editBet(fixture.getModerator().getId(), playerOneBetOne.getId(), editedBet);

        Bet updatedBet = betsRepository.findById(playerOneBetOne.getId()).orElseThrow();
        assertEquals(fixture.getPlayerTwo().getId(), updatedBet.getUser().getId());
        assertEquals(Bet.BetStatus.WON, updatedBet.getBetStatus());

        assertPlayerOneStatsAfterOneWonBetRemains();
        assertPlayerTwoStatsAfterReceivingOneWonBet();
    }

    private void assertPlayerOneStatsAfterTwoWonBets() {
        User playerOne = fixture.getPlayerOne();
        double sumOdds = BET_ONE_ODDS + BET_TWO_ODDS;
        double totalBalance = BET_ONE_BALANCE + BET_TWO_BALANCE;

        assertFullUserStats(
                playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                fixture, playerOne,
                2, 2, 2, sumOdds, totalBalance,
                2, 2, totalBalance
        );
    }

    private void assertPlayerOneStatsAfterOneWonBetRemains() {
        User playerOne = fixture.getPlayerOne();

        assertFullUserStats(
                playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                fixture, playerOne,
                1, 1, 1, BET_TWO_ODDS, BET_TWO_BALANCE,
                1, 1, BET_TWO_BALANCE
        );
    }

    private void assertPlayerTwoHasNoStats() {
        String seasonId = fixture.getSeason().getId();
        String leagueId = fixture.getLeague().getId();
        User playerTwo = fixture.getPlayerTwo();

        assertPlayerStatsAbsent(playerStatsRepository, seasonId, leagueId, playerTwo);
        assertPlayerStatsAbsent(playerStatsRepository, seasonId, TOTAL_ID, playerTwo);
        assertTeamStatsAbsent(playerStatsByTeamsRepository, seasonId, leagueId, playerTwo.getId());
        assertBetTitleStatsAbsent(playerStatsByBetTitlesRepository, seasonId, playerTwo.getId());
    }

    private void assertPlayerTwoStatsAfterReceivingOneWonBet() {
        User playerTwo = fixture.getPlayerTwo();

        assertFullUserStats(
                playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                fixture, playerTwo,
                1, 1, 1, BET_ONE_ODDS, BET_ONE_BALANCE,
                1, 1, BET_ONE_BALANCE
        );
    }
}
