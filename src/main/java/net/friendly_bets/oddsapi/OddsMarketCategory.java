package net.friendly_bets.oddsapi;

import lombok.Getter;

@Getter
public enum OddsMarketCategory {

    MATCH_RESULT(1, false),
    DOUBLE_CHANCE(2, true),
    HANDICAP(3, true),
    TOTALS(4, true),
    /** Рассчитанные кэфы «исход + тотал больше» (Пуассон по смерженным 1X2 и тоталам). */
    RESULT_TOTAL_OVER(5, true),
    /** Рассчитанные кэфы «исход + тотал меньше». */
    RESULT_TOTAL_UNDER(6, true),
    HALF_TIME_RESULT(7, true),
    BTTS(8, true),
    GOALS(9, true),
    EXACT_TOTAL_GOALS(10, true),
    CORRECT_SCORE(11, true),
    TEAM_TOTAL_HOME(12, true),
    TEAM_TOTAL_AWAY(13, true),
    OTHER(30, true),
    EXCLUDED(99, true);

    private final int sortOrder;
    private final boolean collapsedByDefault;

    OddsMarketCategory(int sortOrder, boolean collapsedByDefault) {
        this.sortOrder = sortOrder;
        this.collapsedByDefault = collapsedByDefault;
    }
}
