package net.friendly_bets.oddsapi;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Строки рынка Exact Total Goals: {@code 0 goals}, {@code 2 goals}, {@code 7+ goals}.
 */
public final class OddsExactTotalGoalsParser {

    private static final Pattern GOALS_LABEL = Pattern.compile("^(\\d+)\\+?\\s*goals?$");

    private OddsExactTotalGoalsParser() {
    }

    public static Optional<Integer> parseTotalGoalsLabel(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        Matcher m = GOALS_LABEL.matcher(rawKey.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(m.group(1)));
    }
}
