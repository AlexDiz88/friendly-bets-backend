package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveMinuteLabelResolverTest {

    private static final Instant KICKOFF = Instant.parse("2026-08-03T20:00:00Z");

    @Test
    void resolve_plainMinuteWithinFirstHalf() {
        Instant now = KICKOFF.plusSeconds(30 * 60);
        assertEquals("30'", LiveMinuteLabelResolver.resolve("30'", KICKOFF, now));
    }

    @Test
    void resolve_highMinuteBeforeSecondHalfStart_shows45Plus() {
        Instant now = KICKOFF.plusSeconds(50 * 60);
        assertEquals("45+'", LiveMinuteLabelResolver.resolve("48'", KICKOFF, now));
    }

    @Test
    void resolve_highMinuteAfterSecondHalfStart_showsActualMinute() {
        Instant now = KICKOFF.plusSeconds(70 * 60);
        assertEquals("48'", LiveMinuteLabelResolver.resolve("48'", KICKOFF, now));
    }

    @Test
    void resolve_preservesExplicitStoppageFromProvider() {
        Instant now = KICKOFF.plusSeconds(50 * 60);
        assertEquals("45+3'", LiveMinuteLabelResolver.resolve("45+3'", KICKOFF, now));
        assertEquals("90+6'", LiveMinuteLabelResolver.resolve("90+6'", KICKOFF, now));
    }

    @Test
    void resolve_minuteAbove90_shows90Plus() {
        Instant now = KICKOFF.plusSeconds(120 * 60);
        assertEquals("90+'", LiveMinuteLabelResolver.resolve("93'", KICKOFF, now));
    }

    @Test
    void parseMinuteInteger_handlesStoppage() {
        assertEquals(48, LiveMinuteLabelResolver.parseMinuteInteger("45+3'"));
        assertEquals(72, LiveMinuteLabelResolver.parseMinuteInteger("72'"));
        assertNull(LiveMinuteLabelResolver.parseMinuteInteger(""));
    }

    @Test
    void isLikelyFirstHalfStoppage_usesElapsedTime() {
        assertTrue(LiveMinuteLabelResolver.isLikelyFirstHalfStoppage(48, KICKOFF, KICKOFF.plusSeconds(50 * 60)));
        assertFalse(LiveMinuteLabelResolver.isLikelyFirstHalfStoppage(48, KICKOFF, KICKOFF.plusSeconds(70 * 60)));
    }

    @Test
    void isSupportedLeagueCode() {
        assertTrue(LiveMinuteLabelResolver.isSupportedLeagueCode("EPL"));
        assertFalse(LiveMinuteLabelResolver.isSupportedLeagueCode("UNKNOWN"));
    }
}
