package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamNameNormalizerTest {

    @Test
    void namesMatch_ignoresFcAndCase() {
        assertTrue(TeamNameNormalizer.namesMatch("Manchester United FC", "Manchester United"));
        assertTrue(TeamNameNormalizer.namesMatch("FC Bayern München", "Bayern Munich"));
    }

    @Test
    void namesMatch_differentTeams() {
        assertFalse(TeamNameNormalizer.namesMatch("Arsenal", "Chelsea"));
    }
}
