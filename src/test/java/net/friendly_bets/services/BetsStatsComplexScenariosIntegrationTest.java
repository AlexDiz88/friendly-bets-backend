package net.friendly_bets.services;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.User;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.enums.BetTitleSubCategory;
import net.friendly_bets.repositories.*;
import net.friendly_bets.support.AbstractMongoIntegrationTest;
import net.friendly_bets.support.TestDataFactory;
import net.friendly_bets.support.TwoPlayersTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static net.friendly_bets.support.IntegrationStatsAssertions.*;
import static net.friendly_bets.support.IntegrationTestDtoFactory.*;
import static net.friendly_bets.support.TestDataFactory.defaultLossGameScore;
import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Дополнительные «тяжёлые» интеграционные сценарии: несколько сущностей, полная проверка
 * player / team / bet-title статистики и календаря.
 */
class BetsStatsComplexScenariosIntegrationTest extends AbstractMongoIntegrationTest {

    private static final double ODDS = 2.0;
    private static final int SIZE = 10;
    private static final double WON_BALANCE = ODDS * SIZE - SIZE;
    private static final double LOST_BALANCE = -SIZE;
    private static final double NEW_ODDS = 3.0;
    private static final double NEW_WON_BALANCE = NEW_ODDS * SIZE - SIZE;

    @Autowired
    BetsService betsService;

    @Autowired
    TestDataFactory testData;

    @Autowired
    BetsRepository betsRepository;

    @Autowired
    CalendarsRepository calendarsRepository;

    @Autowired
    PlayerStatsRepository playerStatsRepository;

    @Autowired
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;

    @Autowired
    PlayerStatsByBetTitlesRepository playerStatsByBetTitlesRepository;

    @Nested
    @DisplayName("editBet WON -> LOST")
    class EditBetWonToLost {

        private TwoPlayersTestFixture fixture;
        private Bet wonBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(2);
            wonBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should recalculate player, team and bet-title stats after status change")
        void shouldRecalculateAllStats() {
            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, ODDS, WON_BALANCE, 1, 1, WON_BALANCE);

            betsService.editBet(fixture.getModerator().getId(), wonBet.getId(), editedBetWonToLost(fixture, wonBet));

            assertFullUserStatsWithLost(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, ODDS, LOST_BALANCE, 1, 1, LOST_BALANCE);
        }
    }

    @Nested
    @DisplayName("deleteBet one of two WON bets")
    class DeleteOneOfTwoWonBets {

        private static final double BET_TWO_ODDS = 3.0;
        private static final double BET_TWO_BALANCE = BET_TWO_ODDS * SIZE - SIZE;

        private TwoPlayersTestFixture fixture;
        private Bet betToDelete;
        private Bet betToKeep;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(2);
            betToDelete = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
            betToKeep = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getAwayTeam(), fixture.getHomeTeam(), BET_TWO_ODDS, SIZE);
        }

        @Test
        @DisplayName("should leave stats matching the remaining WON bet only")
        void shouldKeepStatsForRemainingBet() {
            betsService.deleteBet(fixture.getModerator().getId(), betToDelete.getId(), deletedBetFrom(fixture));

            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, BET_TWO_ODDS, BET_TWO_BALANCE, 1, 1, BET_TWO_BALANCE);
        }
    }

    @Nested
    @DisplayName("editBet change away team")
    class EditBetAwayTeamChange {

        private TwoPlayersTestFixture fixture;
        private Bet wonBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersThreeTeamsSetup(2);
            wonBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should move team stats from old away team to new away team")
        void shouldMoveTeamStatsToNewOpponent() {
            String seasonId = fixture.getSeason().getId();
            String leagueId = fixture.getLeague().getId();
            User player = fixture.getPlayerOne();

            assertTeamStats(playerStatsByTeamsRepository, seasonId, leagueId, player.getId(),
                    fixture.getHomeTeam().getId(), 1, 1, WON_BALANCE);
            assertTeamStats(playerStatsByTeamsRepository, seasonId, leagueId, player.getId(),
                    fixture.getAwayTeam().getId(), 1, 1, WON_BALANCE);

            betsService.editBet(
                    fixture.getModerator().getId(),
                    wonBet.getId(),
                    editedBetWithAwayTeam(fixture, wonBet, fixture.getThirdTeam()));

            assertTeamStats(playerStatsByTeamsRepository, seasonId, leagueId, player.getId(),
                    fixture.getHomeTeam().getId(), 1, 1, WON_BALANCE);
            assertTeamStats(playerStatsByTeamsRepository, seasonId, leagueId, player.getId(),
                    fixture.getThirdTeam().getId(), 1, 1, WON_BALANCE);
            assertTeamAbsent(playerStatsByTeamsRepository, seasonId, leagueId, player.getId(),
                    fixture.getAwayTeam().getId());
            assertTeamAbsent(playerStatsByTeamsRepository, seasonId, TOTAL_ID, player.getId(),
                    fixture.getAwayTeam().getId());

            assertPlayerStats(playerStatsRepository, seasonId, leagueId, player,
                    1, 1, 1, ODDS, WON_BALANCE);
            assertBetTitleStats(playerStatsByBetTitlesRepository, seasonId, player.getId(),
                    1, 1, ODDS, WON_BALANCE);
        }
    }

    @Nested
    @DisplayName("editBet change matchDay")
    class EditBetMatchdayChange {

        private TwoPlayersTestFixture fixture;
        private Bet wonBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersTwoMatchdaysSetup(2);
            wonBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(),
                    ODDS, SIZE, fixture.getMatchDay());
        }

        @Test
        @DisplayName("should move bet between matchday nodes without changing player stats")
        void shouldMoveBetBetweenMatchdays() {
            assertEquals(1, countBetsOnMatchday(fixture, fixture.getMatchDay()));
            assertEquals(0, countBetsOnMatchday(fixture, fixture.getSecondMatchDay()));

            betsService.editBet(
                    fixture.getModerator().getId(),
                    wonBet.getId(),
                    editedBetWithMatchDay(fixture, wonBet, fixture.getSecondMatchDay()));

            assertEquals(0, countBetsOnMatchday(fixture, fixture.getMatchDay()));
            assertEquals(1, countBetsOnMatchday(fixture, fixture.getSecondMatchDay()));
            assertEquals(fixture.getSecondMatchDay(),
                    betsRepository.findById(wonBet.getId()).orElseThrow().getMatchDay());

            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, ODDS, WON_BALANCE, 1, 1, WON_BALANCE);
        }
    }

    @Nested
    @DisplayName("editBet change bet title HOME_WIN -> AWAY_WIN")
    class EditBetBetTitleChange {

        private TwoPlayersTestFixture fixture;
        private Bet wonBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(2);
            wonBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should move bet-title stats between subcategories")
        void shouldMoveBetTitleStats() {
            String seasonId = fixture.getSeason().getId();
            User player = fixture.getPlayerOne();

            assertBetTitleSubcategory(playerStatsByBetTitlesRepository, seasonId, player.getId(),
                    BetTitleSubCategory.HOME_WIN, 1, 1, ODDS);

            betsService.editBet(
                    fixture.getModerator().getId(),
                    wonBet.getId(),
                    editedBetWithBetTitle(fixture, wonBet, BetTitleCode.AWAY_WIN, defaultLossGameScore()));

            assertBetTitleSubcategory(playerStatsByBetTitlesRepository, seasonId, player.getId(),
                    BetTitleSubCategory.HOME_WIN, 0, 0, 0);
            assertBetTitleSubcategory(playerStatsByBetTitlesRepository, seasonId, player.getId(),
                    BetTitleSubCategory.AWAY_WIN, 1, 1, ODDS);
            assertBetTitleSummary(playerStatsByBetTitlesRepository, seasonId, player.getId(),
                    1, 1, WON_BALANCE);
        }
    }

    @Nested
    @DisplayName("editBet change odds on WON bet")
    class EditBetOddsChange {

        private TwoPlayersTestFixture fixture;
        private Bet wonBet;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(2);
            wonBet = testData.addOpenedAndWonBet(
                    fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should recalculate balance in all stat layers")
        void shouldRecalculateBalance() {
            betsService.editBet(
                    fixture.getModerator().getId(),
                    wonBet.getId(),
                    editedBetWithOdds(fixture, wonBet, NEW_ODDS, SIZE));

            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, NEW_ODDS, NEW_WON_BALANCE, 1, 1, NEW_WON_BALANCE);
        }
    }

    @Nested
    @DisplayName("two players setBetResult")
    class TwoPlayersSetBetResult {

        private TwoPlayersTestFixture fixture;

        @BeforeEach
        void seed() {
            fixture = testData.createTwoPlayersFirstMatchdaySetup(2);
            testData.addOpenedBet(fixture, fixture.getPlayerOne(), fixture.getHomeTeam(), fixture.getAwayTeam(), ODDS, SIZE);
            testData.addOpenedBet(fixture, fixture.getPlayerTwo(), fixture.getAwayTeam(), fixture.getHomeTeam(), ODDS, SIZE);
        }

        @Test
        @DisplayName("should build full stats for both players after WON results")
        void shouldBuildStatsForBothPlayers() {
            var bets = betsRepository.findAll();
            assertEquals(2, bets.size());

            for (Bet bet : bets) {
                var result = net.friendly_bets.models.BetResult.builder()
                        .gameScore(TestDataFactory.defaultWinGameScore())
                        .betStatus(Bet.BetStatus.WON.name())
                        .build();
                betsService.setBetResult(fixture.getModerator().getId(), bet.getId(), result);
            }

            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerOne(),
                    1, 1, 1, ODDS, WON_BALANCE, 1, 1, WON_BALANCE);
            assertFullUserStats(
                    playerStatsRepository, playerStatsByTeamsRepository, playerStatsByBetTitlesRepository,
                    fixture, fixture.getPlayerTwo(),
                    1, 1, 1, ODDS, WON_BALANCE, 1, 1, WON_BALANCE);
        }
    }

    private int countBetsOnMatchday(TwoPlayersTestFixture fixture, String matchDay) {
        CalendarNode calendarNode = calendarsRepository.findById(fixture.getCalendarNode().getId()).orElseThrow();
        return calendarNode.getLeagueMatchdayNodes().stream()
                .filter(node -> node.getLeagueId().equals(fixture.getLeague().getId())
                        && node.getMatchDay().equals(matchDay))
                .findFirst()
                .orElseThrow()
                .getBets()
                .size();
    }
}
