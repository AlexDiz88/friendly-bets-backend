package net.friendly_bets.support;

import lombok.experimental.UtilityClass;
import net.friendly_bets.models.*;
import net.friendly_bets.models.enums.BetTitleCategory;
import net.friendly_bets.models.enums.BetTitleSubCategory;
import net.friendly_bets.repositories.PlayerStatsByBetTitlesRepository;
import net.friendly_bets.repositories.PlayerStatsByTeamsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;

import static net.friendly_bets.models.enums.BetTitleSubCategory.*;
import static net.friendly_bets.utils.Constants.TOTAL_ID;
import static org.junit.jupiter.api.Assertions.*;

@UtilityClass
public class IntegrationStatsAssertions {

    public static void assertPlayerStatsAbsent(PlayerStatsRepository repository,
                                               String seasonId,
                                               String leagueId,
                                               User user) {
        assertTrue(repository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user).isEmpty(),
                "Expected no player stats for user " + user.getId() + " in league " + leagueId);
    }

    public static void assertPlayerStats(PlayerStatsRepository repository,
                                         String seasonId,
                                         String leagueId,
                                         User user,
                                         int expectedTotalBets,
                                         int expectedBetCount,
                                         int expectedWonBetCount,
                                         double expectedSumOfOdds,
                                         double expectedActualBalance) {
        PlayerStats stats = repository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user)
                .orElseThrow(() -> new AssertionError("PlayerStats not found for user " + user.getId()));

        assertEquals(expectedTotalBets, stats.getTotalBets(), "totalBets");
        assertEquals(expectedBetCount, stats.getBetCount(), "betCount");
        assertEquals(expectedWonBetCount, stats.getWonBetCount(), "wonBetCount");
        assertEquals(0, stats.getLostBetCount(), "lostBetCount");
        assertEquals(expectedSumOfOdds, stats.getSumOfOdds(), 0.001, "sumOfOdds");
        assertEquals(expectedActualBalance, stats.getActualBalance(), 0.001, "actualBalance");
    }

    public static void assertTeamStats(PlayerStatsByTeamsRepository repository,
                                       String seasonId,
                                       String leagueId,
                                       String userId,
                                       String teamId,
                                       int expectedBetCount,
                                       int expectedWonBetCount,
                                       double expectedActualBalance) {
        PlayerStatsByTeams statsByTeams = repository
                .findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId)
                .orElseThrow(() -> new AssertionError("Team stats not found for user " + userId));

        TeamStats teamStats = statsByTeams.getTeamStats().stream()
                .filter(ts -> ts.getTeam().getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Team stats not found for team " + teamId));

        assertEquals(expectedBetCount, teamStats.getBetCount(), "team betCount");
        assertEquals(expectedWonBetCount, teamStats.getWonBetCount(), "team wonBetCount");
        assertEquals(expectedActualBalance, teamStats.getActualBalance(), 0.001, "team actualBalance");
    }

    public static void assertTeamStatsAbsent(PlayerStatsByTeamsRepository repository,
                                             String seasonId,
                                             String leagueId,
                                             String userId) {
        assertTrue(repository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId).isEmpty(),
                "Expected no team stats for user " + userId);
    }

    public static void assertBetTitleStats(PlayerStatsByBetTitlesRepository repository,
                                           String seasonId,
                                           String userId,
                                           int expectedBetCount,
                                           int expectedWonBetCount,
                                           double expectedSumOfOdds,
                                           double expectedActualBalance) {
        assertBetTitleSubcategory(repository, seasonId, userId, HOME_WIN,
                expectedBetCount, expectedWonBetCount, expectedSumOfOdds);
        assertBetTitleSummary(repository, seasonId, userId,
                expectedBetCount, expectedWonBetCount, expectedActualBalance);
    }

    public static void assertBetTitleSubcategory(PlayerStatsByBetTitlesRepository repository,
                                                 String seasonId,
                                                 String userId,
                                                 BetTitleSubCategory subCategory,
                                                 int expectedBetCount,
                                                 int expectedWonBetCount,
                                                 double expectedSumOfOdds) {
        PlayerStatsByBetTitles stats = repository.findBySeasonIdAndUserId(seasonId, userId)
                .orElseThrow(() -> new AssertionError("Bet title stats not found for user " + userId));

        BetTitleSubcategoryStats sub = findSubcategoryStats(stats, BetTitleCategory.GAME_RESULTS, subCategory);
        assertEquals(expectedBetCount, sub.getBetCount(), subCategory + " betCount");
        assertEquals(expectedWonBetCount, sub.getWonBetCount(), subCategory + " wonBetCount");
        assertEquals(expectedSumOfOdds, sub.getSumOfOdds(), 0.001, subCategory + " sumOfOdds");
    }

    public static void assertBetTitleSummary(PlayerStatsByBetTitlesRepository repository,
                                             String seasonId,
                                             String userId,
                                             int expectedBetCount,
                                             int expectedWonBetCount,
                                             double expectedActualBalance) {
        PlayerStatsByBetTitles stats = repository.findBySeasonIdAndUserId(seasonId, userId)
                .orElseThrow(() -> new AssertionError("Bet title stats not found for user " + userId));

        assertEquals(expectedActualBalance, stats.getActualBalance(), 0.001, "betTitle actualBalance");

        BetTitleSubcategoryStats summary = findSubcategoryStats(stats, BetTitleCategory.GAME_RESULTS, SUMMARY);
        assertEquals(expectedBetCount, summary.getBetCount(), "SUMMARY betCount");
        assertEquals(expectedWonBetCount, summary.getWonBetCount(), "SUMMARY wonBetCount");
    }

    public static void assertTeamAbsent(PlayerStatsByTeamsRepository repository,
                                        String seasonId,
                                        String leagueId,
                                        String userId,
                                        String teamId) {
        repository.findBySeasonIdAndLeagueIdAndUserId(seasonId, leagueId, userId).ifPresent(statsByTeams -> {
            boolean present = statsByTeams.getTeamStats().stream()
                    .anyMatch(ts -> ts.getTeam().getId().equals(teamId));
            assertFalse(present, "Expected no stats for team " + teamId);
        });
    }

    public static void assertBetTitleStatsAbsent(PlayerStatsByBetTitlesRepository repository,
                                                 String seasonId,
                                                 String userId) {
        assertTrue(repository.findBySeasonIdAndUserId(seasonId, userId).isEmpty(),
                "Expected no bet title stats for user " + userId);
    }

    public static void assertFullUserStats(PlayerStatsRepository playerStatsRepository,
                                           PlayerStatsByTeamsRepository teamStatsRepository,
                                           PlayerStatsByBetTitlesRepository betTitleStatsRepository,
                                           TwoPlayersTestFixture fixture,
                                           User user,
                                           int expectedTotalBets,
                                           int expectedBetCount,
                                           int expectedWonBetCount,
                                           double expectedSumOfOdds,
                                           double expectedActualBalance,
                                           int expectedHomeTeamBetCount,
                                           int expectedAwayTeamBetCount,
                                           double expectedTeamActualBalance) {
        String seasonId = fixture.getSeason().getId();
        String leagueId = fixture.getLeague().getId();
        String homeTeamId = fixture.getHomeTeam().getId();
        String awayTeamId = fixture.getAwayTeam().getId();

        assertPlayerStats(playerStatsRepository, seasonId, leagueId, user,
                expectedTotalBets, expectedBetCount, expectedWonBetCount, expectedSumOfOdds, expectedActualBalance);
        assertPlayerStats(playerStatsRepository, seasonId, TOTAL_ID, user,
                expectedTotalBets, expectedBetCount, expectedWonBetCount, expectedSumOfOdds, expectedActualBalance);

        // В team stats баланс матча начисляется и домашней, и гостевой команде полностью
        assertTeamStats(teamStatsRepository, seasonId, leagueId, user.getId(), homeTeamId,
                expectedHomeTeamBetCount, expectedHomeTeamBetCount, expectedTeamActualBalance);
        assertTeamStats(teamStatsRepository, seasonId, leagueId, user.getId(), awayTeamId,
                expectedAwayTeamBetCount, expectedAwayTeamBetCount, expectedTeamActualBalance);

        assertBetTitleStats(betTitleStatsRepository, seasonId, user.getId(),
                expectedBetCount, expectedWonBetCount, expectedSumOfOdds, expectedActualBalance);
    }

    public static void assertFullUserStatsWithLost(PlayerStatsRepository playerStatsRepository,
                                                   PlayerStatsByTeamsRepository teamStatsRepository,
                                                   PlayerStatsByBetTitlesRepository betTitleStatsRepository,
                                                   TwoPlayersTestFixture fixture,
                                                   User user,
                                                   int expectedTotalBets,
                                                   int expectedBetCount,
                                                   int expectedLostBetCount,
                                                   double expectedSumOfOdds,
                                                   double expectedActualBalance,
                                                   int expectedHomeTeamBetCount,
                                                   int expectedAwayTeamBetCount,
                                                   double expectedTeamActualBalance) {
        String seasonId = fixture.getSeason().getId();
        String leagueId = fixture.getLeague().getId();
        String homeTeamId = fixture.getHomeTeam().getId();
        String awayTeamId = fixture.getAwayTeam().getId();

        assertPlayerStatsWithLost(playerStatsRepository, seasonId, leagueId, user,
                expectedTotalBets, expectedBetCount, expectedLostBetCount, expectedSumOfOdds, expectedActualBalance);
        assertPlayerStatsWithLost(playerStatsRepository, seasonId, TOTAL_ID, user,
                expectedTotalBets, expectedBetCount, expectedLostBetCount, expectedSumOfOdds, expectedActualBalance);

        assertTeamStats(teamStatsRepository, seasonId, leagueId, user.getId(), homeTeamId,
                expectedHomeTeamBetCount, 0, expectedTeamActualBalance);
        assertTeamStats(teamStatsRepository, seasonId, leagueId, user.getId(), awayTeamId,
                expectedAwayTeamBetCount, 0, expectedTeamActualBalance);

        assertBetTitleSubcategory(betTitleStatsRepository, seasonId, user.getId(), HOME_WIN,
                expectedBetCount, 0, expectedSumOfOdds);
        assertBetTitleSummary(betTitleStatsRepository, seasonId, user.getId(),
                expectedBetCount, 0, expectedActualBalance);
    }

    public static void assertPlayerStatsWithLost(PlayerStatsRepository repository,
                                                 String seasonId,
                                                 String leagueId,
                                                 User user,
                                                 int expectedTotalBets,
                                                 int expectedBetCount,
                                                 int expectedLostBetCount,
                                                 double expectedSumOfOdds,
                                                 double expectedActualBalance) {
        PlayerStats stats = repository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user)
                .orElseThrow(() -> new AssertionError("PlayerStats not found for user " + user.getId()));

        assertEquals(expectedTotalBets, stats.getTotalBets(), "totalBets");
        assertEquals(expectedBetCount, stats.getBetCount(), "betCount");
        assertEquals(0, stats.getWonBetCount(), "wonBetCount");
        assertEquals(expectedLostBetCount, stats.getLostBetCount(), "lostBetCount");
        assertEquals(expectedSumOfOdds, stats.getSumOfOdds(), 0.001, "sumOfOdds");
        assertEquals(expectedActualBalance, stats.getActualBalance(), 0.001, "actualBalance");
    }

    private static BetTitleSubcategoryStats findSubcategoryStats(PlayerStatsByBetTitles stats,
                                                                   BetTitleCategory category,
                                                                   BetTitleSubCategory subCategory) {
        return stats.getBetTitleCategoryStats().stream()
                .filter(cat -> cat.getCategory() == category)
                .flatMap(cat -> cat.getStats().stream())
                .filter(sub -> sub.getSubCategory() == subCategory)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Subcategory not found: " + category + " / " + subCategory));
    }
}
