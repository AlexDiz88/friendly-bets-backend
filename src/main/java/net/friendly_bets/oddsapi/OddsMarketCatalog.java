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
        if (name.equals("half time result")) {
            return OddsMarketCategory.MATCH_RESULT;
        }
        if (name.equals("double chance")) {
            return OddsMarketCategory.DOUBLE_CHANCE;
        }
        if (name.contains("european handicap")) {
            return OddsMarketCategory.EXCLUDED;
        }
        if (isHandicapMarket(name)) {
            return OddsMarketCategory.HANDICAP;
        }
        if (name.equals("totals") || name.equals("goals over/under")
                || name.equals("alternative total goals")) {
            return OddsMarketCategory.TOTALS;
        }
        if (name.equals("exact total goals")) {
            return OddsMarketCategory.EXACT_TOTAL_GOALS;
        }
        if (name.equals("alternative goal line")) {
            return OddsMarketCategory.EXCLUDED;
        }
        if (name.startsWith("both teams to score")) {
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

    private static boolean isHandicapMarket(String name) {
        return name.equals("spread")
                || name.equals("handicap")
                || name.contains("asian handicap");
    }

    private static boolean isHalfTimeOrExcludedVariant(String name) {
        if (name.startsWith("both teams to score") || name.equals("half time result")) {
            return false;
        }
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
            case PERIOD_HANDICAP -> "periodHandicap";
            case TOTALS -> "totals";
            case HALF_TOTALS -> "halfTotals";
            case HALF_FULL -> "halfFull";
            case FIRST_SECOND_HALF -> "firstSecondHalf";
            case RESULT_TOTAL_OVER -> "resultTotalOver";
            case RESULT_TOTAL_UNDER -> "resultTotalUnder";
            case BTTS -> "btts";
            case RESULT_BTTS -> "resultBtts";
            case GOALS -> "goals";
            case EXACT_TOTAL_GOALS -> "exactTotalGoals";
            case TEAM_TOTAL_HOME -> "teamTotalHome";
            case TEAM_TOTAL_AWAY -> "teamTotalAway";
            case CORRECT_SCORE -> "correctScore";
            case CLEAN_WIN -> "cleanWin";
            case WIN_GOAL_DIFFERENCE -> "winGoalDifference";
            case PLAYOFF_EXTRA_TIME -> "playoffExtraTime";
            case FIRST_HALF_CORRECT_SCORE -> "firstHalfCorrectScore";
            case SECOND_HALF_CORRECT_SCORE -> "secondHalfCorrectScore";
            case OTHER -> "other";
            case EXCLUDED -> "excluded";
        };
    }

    public static boolean isFirstHalfResultMarket(String marketName) {
        return marketName != null
                && "half time result".equals(marketName.trim().toLowerCase(Locale.ROOT));
    }
}
