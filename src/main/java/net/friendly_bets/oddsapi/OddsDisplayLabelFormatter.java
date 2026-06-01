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
        BetTitle betTitle = row.getBetTitle();
        if (betTitle != null && betTitle.getLabel() != null && !betTitle.getLabel().isBlank()) {
            return formatFromBetTitle(category, betTitle.getLabel());
        }
        return formatRaw(category, row);
    }

    private static String formatFromBetTitle(OddsMarketCategory category, String label) {
        return switch (category) {
            case HANDICAP -> formatHandicapLabel(label);
            case TEAM_TOTAL_HOME, TEAM_TOTAL_AWAY -> shortenTeamTotalLabel(label);
            default -> label;
        };
    }

    private static String formatRaw(OddsMarketCategory category, OddsLineRow row) {
        String line = row.getLine();
        String selection = row.getSelectionCode();
        if (selection == null) {
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : "";
        }
        OddsSelectionCode code;
        try {
            code = OddsSelectionCode.valueOf(selection);
        } catch (IllegalArgumentException e) {
            return row.getDisplayLabel() != null ? row.getDisplayLabel() : selection;
        }
        return switch (category) {
            case HANDICAP -> formatHandicapRaw(line, code);
            case TOTALS -> formatTotalRaw(line, code);
            case TEAM_TOTAL_HOME, TEAM_TOTAL_AWAY -> formatTeamTotalRaw(line, code);
            default -> code.displayLabel();
        };
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

    private static String formatHandicapRaw(String line, OddsSelectionCode code) {
        String side = code == OddsSelectionCode.HOME ? "Ф1" : "Ф2";
        boolean home = code == OddsSelectionCode.HOME;
        double effective = OddsHandicapLine.effectiveLine(line, home);
        if (Math.abs(effective) < 1e-9 && (line == null || line.isBlank())) {
            return side;
        }
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
