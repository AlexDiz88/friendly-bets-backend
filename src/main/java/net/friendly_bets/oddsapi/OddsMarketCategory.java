package net.friendly_bets.oddsapi;

import lombok.Getter;

@Getter
public enum OddsMarketCategory {

    MATCH_RESULT(1, false),
    DOUBLE_CHANCE(2, true),
    HANDICAP(3, true),
    /** Форы 1-го / 2-го тайма (Marathonbet MTCH_HB1 / MTCH_HB2). */
    PERIOD_HANDICAP(4, true),
    TOTALS(5, true),
    /** «Исход + тотал больше» (скрап Marathonbet). */
    RESULT_TOTAL_OVER(6, true),
    /** «Исход + тотал меньше» (скрап Marathonbet). */
    RESULT_TOTAL_UNDER(7, true),
    CORRECT_SCORE(8, true),
    FIRST_HALF_CORRECT_SCORE(9, true),
    SECOND_HALF_CORRECT_SCORE(10, true),
    /** Тайм/Матч (HT/FT). */
    HALF_FULL(11, true),
    /** 1-й тайм / 2-й тайм. */
    FIRST_SECOND_HALF(12, true),
    BTTS(13, true),
    /** Результат матча + обе забьют. */
    RESULT_BTTS(14, true),
    GOALS(15, true),
    /** Тоталы 1-го и 2-го тайма. */
    HALF_TOTALS(16, true),
    EXACT_TOTAL_GOALS(17, true),
    CLEAN_WIN(18, true),
    WIN_GOAL_DIFFERENCE(19, true),
    TEAM_TOTAL_HOME(27, true),
    TEAM_TOTAL_AWAY(28, true),
    OTHER(30, true),
    EXCLUDED(99, true);

    private final int sortOrder;
    private final boolean collapsedByDefault;

    OddsMarketCategory(int sortOrder, boolean collapsedByDefault) {
        this.sortOrder = sortOrder;
        this.collapsedByDefault = collapsedByDefault;
    }
}
