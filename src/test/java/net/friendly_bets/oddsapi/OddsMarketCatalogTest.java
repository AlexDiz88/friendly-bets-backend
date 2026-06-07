package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsMarketCatalogTest {

    @Test
    void resolvesHandicapAliases() {
        assertEquals(OddsMarketCategory.HANDICAP, OddsMarketCatalog.resolveCategory("Spread"));
        assertEquals(OddsMarketCategory.HANDICAP, OddsMarketCatalog.resolveCategory("Handicap"));
        assertEquals(OddsMarketCategory.HANDICAP, OddsMarketCatalog.resolveCategory("Alternative Asian Handicap"));
    }

    @Test
    void resolvesHalfTimeMarkets() {
        assertEquals(OddsMarketCategory.MATCH_RESULT, OddsMarketCatalog.resolveCategory("Half Time Result"));
        assertEquals(OddsMarketCategory.BTTS, OddsMarketCatalog.resolveCategory("Both Teams To Score HT"));
        assertEquals(OddsMarketCategory.BTTS, OddsMarketCatalog.resolveCategory("Both Teams To Score 2H"));
    }

    @Test
    void excludesEuropeanHandicap() {
        assertEquals(OddsMarketCategory.EXCLUDED, OddsMarketCatalog.resolveCategory("European Handicap"));
    }

    @Test
    void resolvesTotalsAliases() {
        assertEquals(OddsMarketCategory.TOTALS, OddsMarketCatalog.resolveCategory("Goals Over/Under"));
    }
}
