package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsMarketFilterTest {

    @Test
    void excludesCornerMarkets() {
        assertFalse(OddsMarketFilter.isMarketAllowed("Corners Spread"));
        assertFalse(OddsMarketFilter.isMarketAllowed("Corners Totals Home"));
    }

    @Test
    void allowsMainMarkets() {
        assertTrue(OddsMarketFilter.isMarketAllowed("ML"));
        assertTrue(OddsMarketFilter.isMarketAllowed("Totals"));
        assertTrue(OddsMarketFilter.isMarketAllowed("Both Teams To Score"));
    }

    @Test
    void excludesQuarterAsianLines() {
        assertTrue(OddsMarketFilter.isQuarterAsianLine("2.25"));
        assertTrue(OddsMarketFilter.isQuarterAsianLine("1.75"));
        assertFalse(OddsMarketFilter.isQuarterAsianLine("2.5"));
        assertFalse(OddsMarketFilter.isQuarterAsianLine("2"));
    }

    @Test
    void excludesLatencyBookmakerKeys() {
        assertFalse(OddsMarketFilter.isBookmakerKeyAllowed("Bet365 (no latency)"));
        assertTrue(OddsMarketFilter.isBookmakerKeyAllowed("Bet365"));
        assertTrue(OddsMarketFilter.isBookmakerKeyAllowed("1xbet"));
    }
}
