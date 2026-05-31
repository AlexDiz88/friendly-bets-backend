package net.friendly_bets.oddsapi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OddsCorrectScoreUtils {

    private static final Pattern SCORE = Pattern.compile("^(\\d+)\\s*[-:]\\s*(\\d+)$");

    private OddsCorrectScoreUtils() {
    }

    static int[] parseScore(String selection) {
        if (selection == null || selection.isBlank()) {
            return null;
        }
        Matcher m = SCORE.matcher(selection.trim());
        if (!m.matches()) {
            return null;
        }
        return new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) };
    }

    /**
     * Порядок: сумма голов ↑, |h−a| ↑, голы хозяев ↓, гостей ↑.
     * 0-0, 1-0, 0-1, 1-1, 2-0, …
     */
    static int sortKey(String selection) {
        int[] score = parseScore(selection);
        if (score == null) {
            return Integer.MAX_VALUE;
        }
        int home = score[0];
        int away = score[1];
        int total = home + away;
        int diff = Math.abs(home - away);
        return total * 100_000 + diff * 1_000 + (100 - home) * 10 + away;
    }
}
