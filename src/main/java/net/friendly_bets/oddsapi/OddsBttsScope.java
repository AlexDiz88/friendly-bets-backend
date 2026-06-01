package net.friendly_bets.oddsapi;

import java.util.Locale;

/** Период для рынка «Обе забьют» (матч / 1-й тайм / 2-й тайм). */
public enum OddsBttsScope {

    FULL(0),
    FIRST_HALF(1),
    SECOND_HALF(2);

    private final int sortOrder;

    OddsBttsScope(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static OddsBttsScope fromMarketName(String marketName) {
        if (marketName == null || marketName.isBlank()) {
            return FULL;
        }
        String name = marketName.trim().toLowerCase(Locale.ROOT);
        if (name.contains("2h") || name.contains("2nd half")) {
            return SECOND_HALF;
        }
        if (name.contains("ht") || name.contains("1st half")) {
            return FIRST_HALF;
        }
        return FULL;
    }

    public static OddsBttsScope fromSelectionCode(String selectionCode) {
        if (selectionCode == null) {
            return FULL;
        }
        if (selectionCode.endsWith("_1H")) {
            return FIRST_HALF;
        }
        if (selectionCode.endsWith("_2H")) {
            return SECOND_HALF;
        }
        return FULL;
    }

    public static String baseSelectionCode(String selectionCode) {
        if (selectionCode == null) {
            return "";
        }
        if (selectionCode.endsWith("_1H")) {
            return selectionCode.substring(0, selectionCode.length() - 3);
        }
        if (selectionCode.endsWith("_2H")) {
            return selectionCode.substring(0, selectionCode.length() - 3);
        }
        return selectionCode;
    }

    public String selectionCode(OddsSelectionCode code) {
        return switch (this) {
            case FULL -> code.name();
            case FIRST_HALF -> code.name() + "_1H";
            case SECOND_HALF -> code.name() + "_2H";
        };
    }

    public String rowKey(OddsSelectionCode code) {
        return name() + "|" + code.name();
    }
}
