package net.friendly_bets.oddsapi;

import net.friendly_bets.gameresults.LeagueCompetitionMapping;
import net.friendly_bets.models.League;
import net.friendly_bets.oddsapi.config.OddsApiProperties;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps {@link League.LeagueCode} to odds-api.io league slugs.
 * Defaults can be overridden via {@code odds-api.league-slugs.*} in application properties.
 */
public final class OddsApiLeagueMapping {

    private static final Map<League.LeagueCode, String> DEFAULT_SLUGS = new EnumMap<>(League.LeagueCode.class);

    static {
        DEFAULT_SLUGS.put(League.LeagueCode.EPL, "england-premier-league");
        DEFAULT_SLUGS.put(League.LeagueCode.BL, "germany-bundesliga");
        DEFAULT_SLUGS.put(League.LeagueCode.CL, "international-clubs-uefa-champions-league");
        DEFAULT_SLUGS.put(League.LeagueCode.WC, "international-world-cup");
        DEFAULT_SLUGS.put(League.LeagueCode.EC, "euro-championship");
    }

    private OddsApiLeagueMapping() {
    }

    public static Optional<String> toLeagueSlug(League.LeagueCode leagueCode, OddsApiProperties properties) {
        if (leagueCode == null || !LeagueCompetitionMapping.isSupported(leagueCode)) {
            return Optional.empty();
        }
        if (properties != null && properties.getLeagueSlugs() != null) {
            String override = properties.getLeagueSlugs().get(leagueCode.name());
            if (override != null && !override.isBlank()) {
                return Optional.of(override.trim());
            }
        }
        return Optional.ofNullable(DEFAULT_SLUGS.get(leagueCode));
    }
}
