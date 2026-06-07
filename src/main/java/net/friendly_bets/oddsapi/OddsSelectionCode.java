package net.friendly_bets.oddsapi;

import lombok.Getter;

@Getter
public enum OddsSelectionCode {

    HOME,
    DRAW,
    AWAY,
    OVER,
    UNDER,
    YES,
    NO,
    DC_1X,
    DC_12,
    DC_X2,
    OTHER;

    public String displayLabel() {
        return switch (this) {
            case HOME -> "П1";
            case DRAW -> "Х";
            case AWAY -> "П2";
            case OVER -> "Б";
            case UNDER -> "М";
            case YES -> "Да";
            case NO -> "Нет";
            case DC_1X -> "1X";
            case DC_12 -> "12";
            case DC_X2 -> "X2";
            case OTHER -> "?";
        };
    }

    public int orderWithinGroup(OddsMarketCategory category) {
        if (category == OddsMarketCategory.DOUBLE_CHANCE) {
            return switch (this) {
                case DC_1X -> 1;
                case DC_12 -> 2;
                case DC_X2 -> 3;
                default -> 99;
            };
        }
        if (category == OddsMarketCategory.MATCH_RESULT) {
            return switch (this) {
                case HOME -> 1;
                case DRAW -> 2;
                case AWAY -> 3;
                default -> 99;
            };
        }
        if (category == OddsMarketCategory.TOTALS || category == OddsMarketCategory.HALF_TOTALS
                || category == OddsMarketCategory.TEAM_TOTAL_HOME
                || category == OddsMarketCategory.TEAM_TOTAL_AWAY) {
            return switch (this) {
                case UNDER -> 1;
                case OVER -> 2;
                default -> 99;
            };
        }
        if (category == OddsMarketCategory.HANDICAP) {
            return switch (this) {
                case HOME -> 1;
                case AWAY -> 2;
                default -> 99;
            };
        }
        if (category == OddsMarketCategory.BTTS) {
            return switch (this) {
                case YES -> 1;
                case NO -> 2;
                default -> 99;
            };
        }
        return 99;
    }
}
