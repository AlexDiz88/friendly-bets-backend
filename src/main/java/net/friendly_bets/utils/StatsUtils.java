package net.friendly_bets.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.models.Stats;

@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsUtils {


    public static void recalculateStats(Stats stats) {
        stats.calculateWinRate();
        stats.calculateAverageOdds();
        stats.calculateAverageWonBetOdds();
    }

    // ------------------------------------------------------------------------------------------------------ //

    public static void updateBetCount(Stats stats, boolean isPlus) {
        int value = isPlus ? 1 : -1;
        stats.setBetCount(stats.getBetCount() + value);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public static void updateBetCountValuesBasedOnBetStatus(Stats stats, Bet.BetStatus betStatus, Double betOdds, boolean isPlus) {
        int modifier = isPlus ? 1 : -1;
        switch (betStatus) {
            case WON:
                stats.setWonBetCount(stats.getWonBetCount() + modifier);
                stats.setSumOfWonOdds(stats.getSumOfWonOdds() + betOdds * modifier);
                break;
            case RETURNED:
                stats.setReturnedBetCount(stats.getReturnedBetCount() + modifier);
                break;
            case LOST:
                stats.setLostBetCount(stats.getLostBetCount() + modifier);
                break;
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    public static void updateSumOfOddsAndActualBalance(Stats stats, Double betOdds, Double balanceChange, boolean isPlus) {
        int modifier = isPlus ? 1 : -1;
        stats.setSumOfOdds(stats.getSumOfOdds() + betOdds * modifier);
        stats.setActualBalance(stats.getActualBalance() + balanceChange * modifier);
    }

    // ------------------------------------------------------------------------------------------------------ //

    public static void updateEmptyBetValues(PlayerStats playerStats, Integer betSize, boolean isPlus) {
        int modifier = isPlus ? 1 : -1;
        playerStats.setBetCount(playerStats.getBetCount() + modifier);
        playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + modifier);
        playerStats.setActualBalance(playerStats.getActualBalance() - Double.valueOf(betSize) * modifier);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
