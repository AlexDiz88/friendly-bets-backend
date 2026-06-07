package net.friendly_bets.marathonbet;

import net.friendly_bets.oddsapi.OddsCorrectScoreUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarathonbetSelectionParsing {

    private static final Pattern HANDICAP = Pattern.compile(
            "(.+?)\\s*\\(([+-]?\\d+(?:[.,]\\d+)?)\\)",
            Pattern.UNICODE_CASE
    );
    private static final Pattern TOTAL = Pattern.compile(
            "(Больше|Меньше)\\s+([\\d.,]+)",
            Pattern.UNICODE_CASE | Pattern.CANON_EQ
    );
    private static final Pattern SCORE_WITH_TEAM = Pattern.compile(
            "^(?:Ничья|.+?)\\s+(\\d+)\\s*-\\s*(\\d+)$",
            Pattern.UNICODE_CASE | Pattern.CANON_EQ
    );
    private static final Pattern RESULT_TOTAL_LINE = Pattern.compile(
            "тотал\\s+(?:меньше|больше|голов)\\s+([\\d.,]+)",
            Pattern.UNICODE_CASE | Pattern.CANON_EQ
    );

    private MarathonbetSelectionParsing() {
    }

    public static Double parseHandicapLine(String selectionName) {
        if (selectionName == null) {
            return null;
        }
        Matcher m = HANDICAP.matcher(selectionName.trim());
        if (!m.matches()) {
            return null;
        }
        return parseDouble(m.group(2));
    }

    public static Double parseTotalLine(String selectionName) {
        if (selectionName == null) {
            return null;
        }
        Matcher m = TOTAL.matcher(selectionName.trim());
        if (!m.matches()) {
            return null;
        }
        return parseDouble(m.group(2));
    }

    public static boolean isTotalOver(String selectionName) {
        if (selectionName == null) {
            return false;
        }
        return selectionName.trim().toLowerCase(Locale.ROOT).startsWith("больше");
    }

    public static Double parseResultTotalLine(String marketName) {
        if (marketName == null) {
            return null;
        }
        Matcher m = RESULT_TOTAL_LINE.matcher(marketName.trim());
        if (!m.find()) {
            return null;
        }
        return parseDouble(m.group(1));
    }

    public static int[] parseCorrectScoreHomeAway(String selectionName, String homeTeam, String awayTeam) {
        if (selectionName == null || selectionName.isBlank()) {
            return null;
        }
        String trimmed = selectionName.trim();
        if ("Счет матча".equalsIgnoreCase(trimmed) || "Счёт матча".equalsIgnoreCase(trimmed)) {
            return null;
        }
        Matcher m = SCORE_WITH_TEAM.matcher(trimmed);
        if (!m.matches()) {
            int[] direct = OddsCorrectScoreUtils.parseScore(trimmed.replace(" ", ""));
            return direct;
        }
        int g1 = Integer.parseInt(m.group(1));
        int g2 = Integer.parseInt(m.group(2));
        if (trimmed.startsWith("Ничья")) {
            return new int[] { g1, g2 };
        }
        String homeNorm = normalizeTeam(homeTeam);
        String awayNorm = normalizeTeam(awayTeam);
        String prefix = trimmed.split("\\s+\\d")[0].trim();
        String prefixNorm = normalizeTeam(prefix);
        if (homeNorm != null && prefixNorm != null && prefixNorm.contains(homeNorm)) {
            return new int[] { g1, g2 };
        }
        if (awayNorm != null && prefixNorm != null && prefixNorm.contains(awayNorm)) {
            return new int[] { g2, g1 };
        }
        return new int[] { g1, g2 };
    }

    public static int correctScoreSortKey(String selectionName) {
        int[] score = OddsCorrectScoreUtils.parseScore(selectionName);
        if (score != null) {
            return OddsCorrectScoreUtils.sortKey(score[0] + "-" + score[1]);
        }
        Matcher m = SCORE_WITH_TEAM.matcher(selectionName != null ? selectionName.trim() : "");
        if (m.matches()) {
            return OddsCorrectScoreUtils.sortKey(m.group(1) + "-" + m.group(2));
        }
        return Integer.MAX_VALUE;
    }

    public static String normalizeTeam(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static double parseDouble(String raw) {
        return Double.parseDouble(raw.trim().replace(',', '.'));
    }
}
