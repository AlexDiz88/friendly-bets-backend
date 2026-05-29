package net.friendly_bets.oddsapi;

import java.util.Locale;

public final class OddsMarketCatalog {

    private OddsMarketCatalog() {
    }

    public static OddsMarketCategory resolveCategory(String rawMarketName) {
        if (rawMarketName == null || rawMarketName.isBlank()) {
            return OddsMarketCategory.EXCLUDED;
        }
        String name = rawMarketName.trim().toLowerCase(Locale.ROOT);

        if (name.equals("ml")) {
            return OddsMarketCategory.MATCH_RESULT;
        }
        if (name.equals("double chance")) {
            return OddsMarketCategory.DOUBLE_CHANCE;
        }
        if (name.contains("european handicap")) {
            return OddsMarketCategory.EXCLUDED;
        }
        if (name.equals("spread") || name.contains("asian handicap")) {
            return OddsMarketCategory.HANDICAP;
        }
        if (name.equals("totals") || name.equals("goals over/under")
                || name.equals("alternative goal line")) {
            return OddsMarketCategory.TOTALS;
        }
        if (name.startsWith("both teams to score") && !name.contains("ht") && !name.contains("2h")) {
            return OddsMarketCategory.BTTS;
        }
        if (name.equals("team total home") || name.equals("team total goals home")) {
            return OddsMarketCategory.TEAM_TOTAL_HOME;
        }
        if (name.equals("team total away") || name.equals("team total goals away")) {
            return OddsMarketCategory.TEAM_TOTAL_AWAY;
        }
        if (name.equals("correct score")) {
            return OddsMarketCategory.CORRECT_SCORE;
        }

        if (isHalfTimeOrExcludedVariant(name)) {
            return OddsMarketCategory.EXCLUDED;
        }

        if (!OddsMarketFilter.isMarketAllowed(rawMarketName)) {
            return OddsMarketCategory.EXCLUDED;
        }

        return OddsMarketCategory.OTHER;
    }

    private static boolean isHalfTimeOrExcludedVariant(String name) {
        return name.contains(" ht") || name.endsWith(" ht")
                || name.contains("half time")
                || name.contains("1st half")
                || name.contains("2nd half")
                || name.contains("2h")
                || name.equals("draw no bet")
                || name.contains("first 10 minutes")
                || name.startsWith("specials");
    }

    /** i18n key suffix: oddsDemo.groups.{key} */
    public static String i18nGroupKey(OddsMarketCategory category) {
        return switch (category) {
            case MATCH_RESULT -> "matchResult";
            case DOUBLE_CHANCE -> "doubleChance";
            case HANDICAP -> "handicap";
            case TOTALS -> "totals";
            case BTTS -> "btts";
            case TEAM_TOTAL_HOME -> "teamTotalHome";
            case TEAM_TOTAL_AWAY -> "teamTotalAway";
            case CORRECT_SCORE -> "correctScore";
            case OTHER -> "other";
            case EXCLUDED -> "excluded";
        };
    }
}
