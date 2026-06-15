package net.friendly_bets.gameresults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MatchDataProvidersTest {

    @Test
    void sourcesStorageKey_hasNoDotsForMongoMapKeys() {
        assertEquals("4score", MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE));
        assertEquals("24score", MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE));
        assertEquals("odds_api", MatchDataProviders.sourcesStorageKey(MatchDataProviders.ODDS_API));
        assertFalse(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE).contains("."));
    }
}
