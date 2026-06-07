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
    /** Тоталы 1-го и 2-го тайма. */
    HALF_TOTALS(7, true),
    /** Тайм/Матч (HT/FT). */
    HALF_FULL(8, true),
    /** 1-й тайм / 2-й тайм. */
    FIRST_SECOND_HALF(9, true),
    BTTS(10, true),
    GOALS(11, true),
    EXACT_TOTAL_GOALS(12, true),
    CORRECT_SCORE(13, true),
    TEAM_TOTAL_HOME(14, true),
    TEAM_TOTAL_AWAY(15, true),
    FIRST_HALF_CORRECT_SCORE(16, true),
    SECOND_HALF_CORRECT_SCORE(17, true),
    OTHER(30, true),
    EXCLUDED(99, true);

    private final int sortOrder;
    private final boolean collapsedByDefault;

    OddsMarketCategory(int sortOrder, boolean collapsedByDefault) {
        this.sortOrder = sortOrder;
        this.collapsedByDefault = collapsedByDefault;
    }
}
