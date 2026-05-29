package net.friendly_bets.oddsapi;

import lombok.Getter;

@Getter
public enum OddsMarketCategory {

    MATCH_RESULT(1, false),
    DOUBLE_CHANCE(2, false),
    HANDICAP(3, false),
    TOTALS(4, false),
    BTTS(5, false),
    TEAM_TOTAL_HOME(10, true),
    TEAM_TOTAL_AWAY(11, true),
    CORRECT_SCORE(20, true),
    OTHER(30, true),
    EXCLUDED(99, true);

    private final int sortOrder;
    private final boolean collapsedByDefault;

    OddsMarketCategory(int sortOrder, boolean collapsedByDefault) {
        this.sortOrder = sortOrder;
        this.collapsedByDefault = collapsedByDefault;
    }
}
