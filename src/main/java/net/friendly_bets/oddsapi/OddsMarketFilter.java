package net.friendly_bets.oddsapi;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Отсекает угловые, фолы и четвертные азиатские линии (2.25, 1.75, …).
 */
public final class OddsMarketFilter {

    private static final Pattern EXCLUDED_MARKET = Pattern.compile(
            "corner|foul|offside|booking|card\\s|cards\\s|throw",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> EXCLUDED_EXACT_MARKETS = Set.of(
            "corners spread",
            "corners totals",
            "corners totals home",
            "corners totals away"
    );

    private OddsMarketFilter() {
    }

    public static boolean isBookmakerKeyAllowed(String bookmakerKey) {
        if (bookmakerKey == null || bookmakerKey.isBlank()) {
            return false;
        }
        String lower = bookmakerKey.toLowerCase(Locale.ROOT);
        return !lower.contains("latency") && !lower.contains("(no ");
    }

    public static boolean isMarketAllowed(String marketName) {
        if (marketName == null || marketName.isBlank()) {
            return false;
        }
        String normalized = marketName.trim().toLowerCase(Locale.ROOT);
        if (EXCLUDED_EXACT_MARKETS.contains(normalized)) {
            return false;
        }
        return !EXCLUDED_MARKET.matcher(normalized).find();
    }

    /**
     * Четвертная азиатская линия: дробная часть 0.25 или 0.75 (например ТБ/фора 2.25).
     * Допускаются целые и «половинные» (.0, .5).
     */
    public static boolean isQuarterAsianLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        try {
            double value = Double.parseDouble(line.trim().replace(',', '.'));
            double fractional = Math.abs(value - Math.rint(value));
            if (fractional < 0.001 || Math.abs(fractional - 0.5) < 0.001) {
                return false;
            }
            return Math.abs(fractional - 0.25) < 0.001 || Math.abs(fractional - 0.75) < 0.001;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isLineAllowed(String line) {
        return !isQuarterAsianLine(line);
    }
}
