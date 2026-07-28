package net.friendly_bets.utils;

import java.util.Locale;

/**
 * Normalizes team display / alias names for auto-bind matching:
 * trim, case-insensitive, ё/е treated as the same letter.
 */
public final class TeamNameNormalizer {

    private TeamNameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }

    public static boolean equalsNormalized(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String na = normalize(a);
        String nb = normalize(b);
        return !na.isEmpty() && na.equals(nb);
    }
}
