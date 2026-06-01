package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.utils.BetCheckUtils;

/**
 * Связка «исход матча + тотал голов» (полный матч, коды 201–800).
 */
record OddsResultTotalCombo(
        BetTitleCode betTitleCode,
        BetCheckUtils.MatchResult matchResult,
        BetCheckUtils.TotalType totalType,
        double totalLine
) {

    static OddsResultTotalCombo fromBetTitleCode(BetTitleCode code) {
        short c = code.getCode();
        if (c < 201 || c > 800) {
            return null;
        }
        return parse(code);
    }

    /** Диапазоны BetTitleCode для «исход + ТБ» (см. BetTitleStatsService). */
    static boolean isOverComboCode(short code) {
        return (code >= 251 && code <= 300)
                || (code >= 351 && code <= 400)
                || (code >= 451 && code <= 500)
                || (code >= 551 && code <= 600)
                || (code >= 651 && code <= 700)
                || (code >= 751 && code <= 800);
    }

    private static OddsResultTotalCombo parse(BetTitleCode code) {
        String name = code.name();
        int andIdx = name.indexOf("_AND_");
        if (andIdx < 0) {
            return null;
        }
        String resultPart = name.substring(0, andIdx);
        String totalPart = name.substring(andIdx + 5);
        BetCheckUtils.MatchResult matchResult = mapMatchResult(resultPart);
        if (matchResult == null) {
            return null;
        }
        boolean over = totalPart.startsWith("OVER_");
        boolean under = totalPart.startsWith("UNDER_");
        if (!over && !under) {
            return null;
        }
        // OVER_ = 5 символов, UNDER_ = 6 (раньше было 7 — ломало линии ТМ)
        String lineToken = totalPart.substring(over ? "OVER_".length() : "UNDER_".length());
        double line = parseLineToken(lineToken);
        if (line < 0) {
            return null;
        }
        return new OddsResultTotalCombo(
                code,
                matchResult,
                over ? BetCheckUtils.TotalType.OVER : BetCheckUtils.TotalType.UNDER,
                line
        );
    }

    private static BetCheckUtils.MatchResult mapMatchResult(String token) {
        return switch (token) {
            case "HOME_WIN" -> BetCheckUtils.MatchResult.HOME_WIN;
            case "DRAW" -> BetCheckUtils.MatchResult.DRAW;
            case "AWAY_WIN" -> BetCheckUtils.MatchResult.AWAY_WIN;
            case "HOME_OR_DRAW" -> BetCheckUtils.MatchResult.HOME_WIN_OR_DRAW;
            case "AWAY_OR_DRAW" -> BetCheckUtils.MatchResult.AWAY_WIN_OR_DRAW;
            case "HOME_OR_AWAY" -> BetCheckUtils.MatchResult.HOME_OR_AWAY_WIN;
            default -> null;
        };
    }

    private static double parseLineToken(String token) {
        try {
            return Double.parseDouble(token.replace('_', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isIntegerLine() {
        return totalLine == Math.floor(totalLine);
    }

    double effectiveTotalWeight(int totalGoals) {
        if (totalType == BetCheckUtils.TotalType.UNDER) {
            if (totalGoals < totalLine) {
                return 1.0;
            }
            if (totalGoals == totalLine && isIntegerLine()) {
                return 1.0;
            }
            return 0.0;
        }
        if (totalGoals > totalLine) {
            return 1.0;
        }
        if (totalGoals == totalLine && isIntegerLine()) {
            return 1.0;
        }
        return 0.0;
    }
}
