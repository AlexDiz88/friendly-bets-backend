package net.friendly_bets.oddsapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OddsBookmakerKeys {

    private OddsBookmakerKeys() {
    }

    /** API key → canonical name from config (Bet365, 1xbet, …). */
    public static Map<String, String> mapApiKeysToConfigured(List<String> configured) {
        Map<String, String> canonicalByLower = new LinkedHashMap<>();
        if (configured != null) {
            for (String name : configured) {
                if (name != null && !name.isBlank()) {
                    canonicalByLower.put(name.toLowerCase(Locale.ROOT), name);
                }
            }
        }
        return canonicalByLower;
    }

    public static String resolveCanonical(String apiBookmakerKey, Map<String, String> canonicalByLower) {
        if (apiBookmakerKey == null || canonicalByLower == null) {
            return null;
        }
        if (!OddsMarketFilter.isBookmakerKeyAllowed(apiBookmakerKey)) {
            return null;
        }
        return canonicalByLower.get(apiBookmakerKey.toLowerCase(Locale.ROOT));
    }
}
