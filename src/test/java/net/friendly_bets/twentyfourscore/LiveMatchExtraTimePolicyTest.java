package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveMatchExtraTimePolicyTest {

    @Test
    void domesticLeaguesNeverAllowExtraTime() {
        assertFalse(LiveMatchExtraTimePolicy.extraTimeAllowed("EPL", "15", "LIVE"));
        assertFalse(LiveMatchExtraTimePolicy.extraTimeAllowed("BL", "12", "LIVE"));
    }

    @Test
    void cupGroupStageDoesNotAllowExtraTime() {
        assertFalse(LiveMatchExtraTimePolicy.extraTimeAllowed("CL", "3", "LIVE"));
        assertFalse(LiveMatchExtraTimePolicy.extraTimeAllowed("WC", "2 [1]", "LIVE"));
    }

    @Test
    void cupKnockoutAllowsExtraTime() {
        assertTrue(LiveMatchExtraTimePolicy.extraTimeAllowed("CL", "1/4", "LIVE"));
        assertTrue(LiveMatchExtraTimePolicy.extraTimeAllowed("WC", "1/8 [1]", "LIVE"));
        assertTrue(LiveMatchExtraTimePolicy.extraTimeAllowed("EC", "1/2", "LIVE"));
    }

    @Test
    void extraTimeStatusOverridesSlot() {
        assertTrue(LiveMatchExtraTimePolicy.extraTimeAllowed("EPL", "15", "EXTRA_TIME"));
    }
}
