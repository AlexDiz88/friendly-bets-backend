package net.friendly_bets.gameresults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MatchDataProvidersTest {

    @Test
    void sourcesStorageKey_hasNoDotsForMongoMapKeys() {
        assertEquals("24score", MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE));
        assertEquals("soccer365", MatchDataProviders.sourcesStorageKey(MatchDataProviders.SOCCER365));
        assertEquals("marathonbet", MatchDataProviders.sourcesStorageKey(MatchDataProviders.MARATHONBET));
        assertFalse(MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE).contains("."));
    }
}
