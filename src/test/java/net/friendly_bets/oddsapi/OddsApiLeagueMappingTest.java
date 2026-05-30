package net.friendly_bets.oddsapi;

import net.friendly_bets.models.League;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsApiLeagueMappingTest {

    @Test
    @DisplayName("WC maps to odds-api.io world-cup slug")
    void wcSlug() {
        assertEquals(
                "international-world-cup",
                OddsApiLeagueMapping.toLeagueSlug(League.LeagueCode.WC, new OddsApiProperties()).orElseThrow()
        );
    }

    @Test
    @DisplayName("property override wins over default slug")
    void propertyOverride() {
        OddsApiProperties properties = new OddsApiProperties();
        properties.getLeagueSlugs().put("WC", "custom-world-cup-slug");

        assertEquals(
                "custom-world-cup-slug",
                OddsApiLeagueMapping.toLeagueSlug(League.LeagueCode.WC, properties).orElseThrow()
        );
    }

    @Test
    @DisplayName("unsupported league has no slug")
    void unsupportedLeague() {
        assertTrue(OddsApiLeagueMapping.toLeagueSlug(League.LeagueCode.LE, new OddsApiProperties()).isEmpty());
    }
}
