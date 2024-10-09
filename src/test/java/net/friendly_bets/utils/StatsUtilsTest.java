package net.friendly_bets.utils;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StatsUtilsTest {

    @Test
    @DisplayName("Constructor should be private")
    void constructor_ShouldBePrivate() throws NoSuchMethodException {
        Constructor<StatsUtils> constructor = StatsUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    @DisplayName("Should invoke calculateWinRate, calculateAverageOdds and calculateAverageWonBetOdds")
    void recalculateStats_ShouldInvokeStatsMethods() {
        // given
        PlayerStats playerStats = spy(PlayerStats.class);
        doNothing().when(playerStats).calculateWinRate();
        doNothing().when(playerStats).calculateAverageOdds();
        doNothing().when(playerStats).calculateAverageWonBetOdds();

        // when
        StatsUtils.recalculateStats(playerStats);

        // then
        verify(playerStats, times(1)).calculateWinRate();
        verify(playerStats, times(1)).calculateAverageOdds();
        verify(playerStats, times(1)).calculateAverageWonBetOdds();
    }

    @ParameterizedTest
    @DisplayName("Should recalculate stats correctly for various cases")
    @MethodSource("recalculateStatsProvider")
    void recalculateStats_ShouldCalculateValuesCorrectly(
            int betCount, int wonBetCount, int returnedBetCount, int emptyBetCount,
            double sumOfOdds, double sumOfWonOdds,
            Double expectedWinRate, Double expectedAverageOdds, Double expectedAverageWonBetOdds) {
        // given
        PlayerStats playerStats = PlayerStats.builder()
                .betCount(betCount)
                .wonBetCount(wonBetCount)
                .returnedBetCount(returnedBetCount)
                .emptyBetCount(emptyBetCount)
                .sumOfOdds(sumOfOdds)
                .sumOfWonOdds(sumOfWonOdds)
                .build();

        // when
        StatsUtils.recalculateStats(playerStats);

        // then
        assertEquals(expectedWinRate, playerStats.getWinRate(), "WinRate");
        assertEquals(expectedAverageOdds, playerStats.getAverageOdds(), "Average Odds");
        assertEquals(expectedAverageWonBetOdds, playerStats.getAverageWonBetOdds(), "Average WonOdds");
    }

    private static Stream<Arguments> recalculateStatsProvider() {
        return Stream.of(
                // usual case
                Arguments.of(10, 5, 1, 1,
                        19.8, 9.0,
                        62.5, 2.2, 1.8),
                // betCount == 0
                Arguments.of(0, 0, 0, 0,
                        0.0, 0.0,
                        0.0, 0.0, 0.0),
                // betCount - emptyBetCount == 0
                Arguments.of(2, 0, 0, 2,
                        0.0, 0.0,
                        0.0, 0.0, 0.0),
                // betCount - returnedBetCount - emptyBetCount == 0
                Arguments.of(10, 0, 5, 5,
                        8.20, 0.0,
                        0.0, 1.64, 0.0),
                // wonBetCount == 0
                Arguments.of(5, 0, 2, 1,
                        7.04, 0.0,
                        0.0, 1.76, 0.0)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @DisplayName("Should update bet count correctly for isPlus true and false")
    @CsvSource({
            "10, true, 11",
            "10, false, 9"
    })
    void updateBetCount_ShouldUpdateBetCountCorrectly(int initialBetCount, boolean isPlus, int expectedBetCount) {
        // given
        PlayerStats playerStats = PlayerStats.builder()
                .betCount(initialBetCount)
                .build();

        // when
        StatsUtils.updateBetCount(playerStats, isPlus);

        // then
        assertEquals(expectedBetCount, playerStats.getBetCount());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @DisplayName("Should update stats correctly based on bet status and isPlus flag")
    @MethodSource("betStatusProvider")
    void updateBetCountValuesBasedOnBetStatus_ShouldUpdateStatsCorrectly(
            Bet.BetStatus betStatus, boolean isPlus, double betOdds,
            int initialWonCount, int initialReturnedCount, int initialLostCount, double initialSumOfWonOdds,
            int expectedWonCount, int expectedReturnedCount, int expectedLostCount, double expectedSumOfWonOdds) {

        // given
        PlayerStats stats = PlayerStats.builder()
                .wonBetCount(initialWonCount)
                .returnedBetCount(initialReturnedCount)
                .lostBetCount(initialLostCount)
                .sumOfWonOdds(initialSumOfWonOdds)
                .build();

        // when
        StatsUtils.updateBetCountValuesBasedOnBetStatus(stats, betStatus, betOdds, isPlus);

        // then
        assertEquals(expectedWonCount, stats.getWonBetCount(), "WonBetCount");
        assertEquals(expectedReturnedCount, stats.getReturnedBetCount(), "ReturnedBetCount");
        assertEquals(expectedLostCount, stats.getLostBetCount(), "LostBetCount");
        assertEquals(expectedSumOfWonOdds, stats.getSumOfWonOdds(), "SumOfWonOdds");
    }

    private static Stream<Arguments> betStatusProvider() {
        return Stream.of(
                Arguments.of(Bet.BetStatus.WON, true, 2.2,
                        5, 2, 3, 5.7,
                        6, 2, 3, 7.9),
                Arguments.of(Bet.BetStatus.WON, false, 2.2,
                        5, 2, 3, 5.7,
                        4, 2, 3, 3.5),
                Arguments.of(Bet.BetStatus.RETURNED, true, 2.0,
                        1, 3, 2, 4.15,
                        1, 4, 2, 4.15),
                Arguments.of(Bet.BetStatus.RETURNED, false, 2.0,
                        1, 3, 2, 4.15,
                        1, 2, 2, 4.15),
                Arguments.of(Bet.BetStatus.LOST, true, 1.6,
                        2, 2, 4, 3.8,
                        2, 2, 5, 3.8),
                Arguments.of(Bet.BetStatus.LOST, false, 1.6,
                        2, 2, 4, 3.8,
                        2, 2, 3, 3.8)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @DisplayName("Should update sum of odds and actual balance based on bet status")
    @MethodSource("sumOfOddsAndBalanceChangeProvider")
    void updateSumOfOddsAndActualBalance_ShouldUpdateValuesCorrectly(
            Bet.BetStatus betStatus, boolean isPlus, double betOdds, double balanceChange,
            double initialSumOfOdds, double initialActualBalance,
            double expectedSumOfOdds, double expectedActualBalance) {
        // given
        PlayerStats stats = PlayerStats.builder()
                .sumOfOdds(initialSumOfOdds)
                .actualBalance(initialActualBalance)
                .build();

        // when
        StatsUtils.updateSumOfOddsAndActualBalance(stats, betStatus, betOdds, balanceChange, isPlus);

        // then
        assertEquals(expectedSumOfOdds, stats.getSumOfOdds(), "SumOfOdds");
        assertEquals(expectedActualBalance, stats.getActualBalance(), "ActualBalance");
    }

    private static Stream<Arguments> sumOfOddsAndBalanceChangeProvider() {
        return Stream.of(
                // Statuses are in WRL_STATUSES
                Arguments.of(Bet.BetStatus.WON, true,
                        2.3, 13.0,
                        25.5, 100.0,
                        27.8, 113.0),
                Arguments.of(Bet.BetStatus.WON, false,
                        2.3, 13.0,
                        25.5, 100.0,
                        23.2, 87.0),
                Arguments.of(Bet.BetStatus.RETURNED, true,
                        3.0, 0.0,
                        30.4, 200.0,
                        33.4, 200.0),
                Arguments.of(Bet.BetStatus.RETURNED, false,
                        3.0, 0.0,
                        30.4, 200.0,
                        27.4, 200.0),
                Arguments.of(Bet.BetStatus.LOST, true,
                        5.4, -10.0,
                        40.6, 150.0,
                        46.0, 140.0),
                Arguments.of(Bet.BetStatus.LOST, false,
                        5.4, -10.0,
                        40.6, 150.0,
                        35.2, 160.0),
                // Status is not in WRL_STATUSES
                Arguments.of(Bet.BetStatus.OPENED, true,
                        2.5, 0.0,
                        10.0, 100.0,
                        10.0, 100.0),
                Arguments.of(Bet.BetStatus.OPENED, false,
                        3.5, 0.0,
                        10.0, 100.0,
                        10.0, 100.0)
        );
    }

    // ------------------------------------------------------------------------------------------------------ //

    @ParameterizedTest
    @DisplayName("Should update bet count, empty bet count and actual balance correctly based on bet size and isPlus")
    @MethodSource("emptyBetValuesProvider")
    void updateEmptyBetValues_ShouldUpdateValuesCorrectly(
            int betSize, boolean isPlus,
            int initialBetCount, int initialEmptyBetCount, double initialActualBalance,
            int expectedBetCount, int expectedEmptyBetCount, double expectedActualBalance) {

        // given
        PlayerStats playerStats = PlayerStats.builder()
                .betCount(initialBetCount)
                .emptyBetCount(initialEmptyBetCount)
                .actualBalance(initialActualBalance)
                .build();

        // when
        StatsUtils.updateEmptyBetValues(playerStats, betSize, isPlus);

        // then
        assertEquals(expectedBetCount, playerStats.getBetCount(), "BetCount");
        assertEquals(expectedEmptyBetCount, playerStats.getEmptyBetCount(), "EmptyBetCount");
        assertEquals(expectedActualBalance, playerStats.getActualBalance(), "ActualBalance");
    }

    private static Stream<Arguments> emptyBetValuesProvider() {
        return Stream.of(
                Arguments.of(10, true,
                        5, 2, 100.0,
                        6, 3, 90.0),
                Arguments.of(10, false,
                        5, 2, 100.0,
                        4, 1, 110.0),
                Arguments.of(20, true,
                        7, 2, -100.0,
                        8, 3, -120.0),
                Arguments.of(20, false,
                        7, 2, -100.0,
                        6, 1, -80.0)
        );
    }
}
