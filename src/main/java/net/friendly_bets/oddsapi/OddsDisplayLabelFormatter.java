package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;

/**
 * Формат подписей строк кэфов для UI выбора ставки.
 */
public final class OddsDisplayLabelFormatter {

    private OddsDisplayLabelFormatter() {
    }

    public static String format(OddsMarketCategory category, OddsLineRow row) {
        if (category == OddsMarketCategory.BTTS) {
            return formatBttsRaw(row);
        }
        if (category == OddsMarketCategory.HANDICAP) {
            return formatHandicapRow(row);
        }
        BetTitle betTitle = row.getBetTitle();
        if (betTitle != null && betTitle.getLabel() != null && !betTitle.getLabel().isBlank()) {
            return formatFromBetTitle(category, betTitle);
        }
        return formatRaw(category, row);
    }

    private static String formatFromBetTitle(OddsMarketCategory category, BetTitle betTitle) {
        String label = betTitle.getLabel();
        return switch (category) {
            case HANDICAP -> formatHandicapLabel(label);
            case TEAM_TOTAL_HOME, TEAM_TOTAL_AWAY -> shortenTeamTotalLabel(label);
            case BTTS, GOALS, RESULT_BTTS, CLEAN_WIN, WIN_GOAL_DIFFERENCE -> formatGoalsBetTitleLabel(betTitle);
            case HALF_FULL, FIRST_SECOND_HALF -> label;
            default -> label;
        };
    }

    private static String formatGoalsBetTitleLabel(BetTitle betTitle) {
        String label = betTitle.getLabel();
        if (betTitle.isNot()) {
            return label + " — нет";
        }
        return label;
    }

    private static String formatRaw(OddsMarketCategory category, OddsLineRow row) {
        String line = row.getLine();
        String selection = row.getSelectionCode();
        if (selection == null) {
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : "";
        }
        if (category == OddsMarketCategory.BTTS) {
            return formatBttsRaw(row);
        }
        OddsSelectionCode code;
        try {
            code = OddsSelectionCode.valueOf(selection);
        } catch (IllegalArgumentException e) {
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : selection;
        }
        return switch (category) {
            case HANDICAP -> formatHandicapRaw(line, code);
            case TOTALS, HALF_TOTALS -> formatTotalRaw(line, code);
            case TEAM_TOTAL_HOME, TEAM_TOTAL_AWAY -> formatTeamTotalRaw(line, code);
            default -> code.displayLabel();
        };
    }

    private static String formatBttsRaw(OddsLineRow row) {
        BetTitle betTitle = row.getBetTitle();
        if (betTitle != null && betTitle.getLabel() != null && !betTitle.getLabel().isBlank()) {
            if (betTitle.isNot()) {
                return betTitle.getLabel() + " — нет";
            }
            return betTitle.getLabel();
        }
        String base = OddsBttsScope.baseSelectionCode(row.getSelectionCode());
        try {
            OddsSelectionCode code = OddsSelectionCode.valueOf(base);
            return code.displayLabel();
        } catch (IllegalArgumentException e) {
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : "";
        }
    }

    /** «Ф1(-2.5)» → «Ф1 (-2.5)» */
    static String formatHandicapLabel(String label) {
        int paren = label.indexOf('(');
        if (paren > 0 && label.charAt(paren - 1) != ' ') {
            return label.substring(0, paren) + " " + label.substring(paren);
        }
        return label;
    }

    /** «Хозяева ИТБ 1.5» → «ИТБ 1.5» */
    static String shortenTeamTotalLabel(String label) {
        if (label.startsWith("Хозяева ")) {
            return label.substring("Хозяева ".length());
        }
        if (label.startsWith("Гости ")) {
            return label.substring("Гости ".length());
        }
        return label;
    }

    private static String formatHandicapRow(OddsLineRow row) {
        OddsSelectionCode code;
        try {
            code = OddsSelectionCode.valueOf(row.getSelectionCode());
        } catch (IllegalArgumentException e) {
            BetTitle betTitle = row.getBetTitle();
            if (betTitle != null && betTitle.getLabel() != null && !betTitle.getLabel().isBlank()) {
                return formatHandicapLabel(betTitle.getLabel());
            }
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : "";
        }
        return formatHandicapRaw(row.getLine(), code);
    }

    private static String formatHandicapRaw(String line, OddsSelectionCode code) {
        String side = code == OddsSelectionCode.HOME ? "Ф1" : "Ф2";
        if (line == null || line.isBlank()) {
            return side;
        }
        double effective = OddsHandicapLine.parse(line);
        return side + " (" + OddsHandicapLine.formatSigned(effective) + ")";
    }

    private static String formatTotalRaw(String line, OddsSelectionCode code) {
        String prefix = code == OddsSelectionCode.OVER ? "ТБ" : "ТМ";
        String formattedLine = formatLineValue(line);
        return formattedLine.isEmpty() ? prefix : prefix + " " + formattedLine;
    }

    private static String formatTeamTotalRaw(String line, OddsSelectionCode code) {
        String prefix = code == OddsSelectionCode.OVER ? "ИТБ" : "ИТМ";
        String formattedLine = formatLineValue(line);
        return formattedLine.isEmpty() ? prefix : prefix + " " + formattedLine;
    }

    private static String formatLineValue(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        try {
            double value = Double.parseDouble(line.trim().replace(',', '.'));
            if (value == Math.floor(value)) {
                return String.valueOf((int) value);
            }
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return line.trim();
        }
    }
}
