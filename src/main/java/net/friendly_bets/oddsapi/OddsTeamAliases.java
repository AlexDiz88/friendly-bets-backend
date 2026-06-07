package net.friendly_bets.oddsapi;

import java.util.Map;

/**
 * Синонимы названий команд в котировках Bet365/1xbet vs БД / odds-api event.
 */
final class OddsTeamAliases {

    private static final Map<String, String> CANONICAL = Map.ofEntries(
            Map.entry("turkiye", "turkey"),
            Map.entry("türkiye", "turkey"),
            Map.entry("czechia", "czech republic"),
            Map.entry("korea republic", "south korea"),
            Map.entry("republic of korea", "south korea"),
            Map.entry("south korea", "south korea"),
            Map.entry("ivory coast", "cote d ivoire"),
            Map.entry("cote divoire", "cote d ivoire"),
            Map.entry("cote d ivoire", "cote d ivoire"),
            Map.entry("usa", "united states"),
            Map.entry("u s a", "united states"),
            Map.entry("bosnia herzegovina", "bosnia and herzegovina")
    );

    private OddsTeamAliases() {
    }

    static String canonicalForMatch(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = TeamNameNormalizer.normalize(raw);
        return CANONICAL.getOrDefault(normalized, normalized);
    }
}
