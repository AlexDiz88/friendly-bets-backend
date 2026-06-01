package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsMarketFilterTest {

    @Test
    void allowsHalfAndIntegerLines() {
        assertTrue(OddsMarketFilter.isLineAllowed("0.5"));
        assertTrue(OddsMarketFilter.isLineAllowed("1.5"));
        assertFalse(OddsMarketFilter.isLineAllowed("2.25"));
    }

    @Test
    void allowsExactAndAlternativeTotalMarkets() {
        assertTrue(OddsMarketFilter.isMarketAllowed("Exact Total Goals"));
        assertTrue(OddsMarketFilter.isMarketAllowed("Alternative Total Goals"));
        assertTrue(OddsMarketFilter.isMarketAllowed("Alternative Asian Handicap"));
    }
}
