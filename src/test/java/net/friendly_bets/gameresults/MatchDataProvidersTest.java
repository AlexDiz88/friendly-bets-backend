package net.friendly_bets.gameresults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchDataProvidersTest {

    @Test
    void sourcesStorageKey_mapsKnownProviders() {
        assertEquals("soccer365", MatchDataProviders.sourcesStorageKey(MatchDataProviders.SOCCER365));
        assertEquals("24score", MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE));
        assertEquals("marathonbet", MatchDataProviders.sourcesStorageKey(MatchDataProviders.MARATHONBET));
        assertNull(MatchDataProviders.sourcesStorageKey(null));
    }
}
