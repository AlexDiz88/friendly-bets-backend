package net.friendly_bets.oddsapi;

import lombok.Getter;

@Getter
public enum OddsMarketCategory {

    MATCH_RESULT(1, false),
    DOUBLE_CHANCE(2, true),
    HANDICAP(3, true),
    TOTALS(4, true),
    /** «Исход + тотал больше» (скрап Marathonbet). */
    RESULT_TOTAL_OVER(5, true),
    /** «Исход + тотал меньше» (скрап Marathonbet). */
    RESULT_TOTAL_UNDER(6, true),
    CORRECT_SCORE(7, true),
    FIRST_HALF_CORRECT_SCORE(8, true),
    SECOND_HALF_CORRECT_SCORE(9, true),
    /** Тайм/Матч (HT/FT). */
    HALF_FULL(10, true),
    /** 1-й тайм / 2-й тайм. */
    FIRST_SECOND_HALF(11, true),
    BTTS(12, true),
    /** Результат матча + обе забьют. */
    RESULT_BTTS(13, true),
    GOALS(14, true),
    /** Тоталы 1-го и 2-го тайма. */
    HALF_TOTALS(15, true),
    EXACT_TOTAL_GOALS(16, true),
    CLEAN_WIN(17, true),
    WIN_GOAL_DIFFERENCE(18, true),
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
