package net.friendly_bets.gameresults;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MatchDataProvidersTest {

    @Test
    void sourcesStorageKey_hasNoDotsForMongoMapKeys() {
        assertEquals("football_data", MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA));
        assertEquals("4score", MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE));
        assertEquals("odds_api", MatchDataProviders.sourcesStorageKey(MatchDataProviders.ODDS_API));
        assertFalse(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE).contains("."));
    }
}
