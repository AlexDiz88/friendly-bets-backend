package net.friendly_bets.oddsapi;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TeamNameNormalizer {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final String[] STRIP_TOKENS = {
            "fc", "cf", "sc", "ac", "afc", "fk", "sk", "sv", "vfb", "vfl", "tsg",
            "united", "city", "town", "rovers", "wanderers", "athletic", "atletico"
    };

    private TeamNameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lowered = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String cleaned = NON_ALNUM.matcher(lowered).replaceAll(" ").trim();
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || isStripToken(part)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }

    public static boolean namesMatch(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isEmpty() || nb.isEmpty()) {
            return false;
        }
        return na.equals(nb) || na.contains(nb) || nb.contains(na);
    }

    private static boolean isStripToken(String token) {
        for (String strip : STRIP_TOKENS) {
            if (strip.equals(token)) {
                return true;
            }
        }
        return false;
    }
}
