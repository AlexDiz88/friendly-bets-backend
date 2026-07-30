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
        assertEquals("45+", LiveMinuteLabelResolver.resolve("48'", KICKOFF, now));
    }

    @Test
    void resolve_highMinuteAfterSecondHalfStart_showsActualMinute() {
        Instant now = KICKOFF.plusSeconds(70 * 60);
        assertEquals("48'", LiveMinuteLabelResolver.resolve("48'", KICKOFF, now));
    }

    @Test
    void resolve_unifiesExplicitStoppageFromProvider() {
        Instant now = KICKOFF.plusSeconds(50 * 60);
        assertEquals("45+", LiveMinuteLabelResolver.resolve("45+3'", KICKOFF, now));
        assertEquals("90+", LiveMinuteLabelResolver.resolve("90+6'", KICKOFF, now));
        assertEquals("45+", LiveMinuteLabelResolver.resolve("45+", KICKOFF, now));
    }

    @Test
    void resolve_minute98InLeagueIsAlways90Plus() {
        Instant now = KICKOFF.plusSeconds(125 * 60);
        assertEquals("90+", LiveMinuteLabelResolver.resolve(
                "98'", KICKOFF, now, "EPL", "15", "LIVE"));
    }

    @Test
    void resolve_minute98BeforeRegulationEndInKnockoutIs90Plus() {
        Instant now = KICKOFF.plusSeconds(110 * 60);
        assertEquals("90+", LiveMinuteLabelResolver.resolve(
                "98'", KICKOFF, now, "CL", "1/4", "LIVE"));
    }

    @Test
    void resolve_minute98AfterRegulationEndInKnockoutIsExtraTimeMinute() {
        Instant now = KICKOFF.plusSeconds(125 * 60);
        assertEquals("98'", LiveMinuteLabelResolver.resolve(
                "98'", KICKOFF, now, "CL", "1/4", "LIVE"));
    }

    @Test
    void resolve_minuteAbove90_shows90PlusWhenNoExtraTime() {
        Instant now = KICKOFF.plusSeconds(120 * 60);
        assertEquals("90+", LiveMinuteLabelResolver.resolve("93'", KICKOFF, now, "BL", "10", "LIVE"));
    }

    @Test
    void resolve_overtimeStoppageLabels() {
        Instant now = KICKOFF.plusSeconds(135 * 60);
        assertEquals("105+", LiveMinuteLabelResolver.resolve(
                "106'", KICKOFF, now, "CL", "final", "EXTRA_TIME"));
    }

    @Test
    void parseMinuteInteger_handlesStoppage() {
        assertEquals(48, LiveMinuteLabelResolver.parseMinuteInteger("45+3'"));
        assertEquals(72, LiveMinuteLabelResolver.parseMinuteInteger("72'"));
        assertNull(LiveMinuteLabelResolver.parseMinuteInteger(""));
    }

    @Test
    void isLikelyFirstHalfStoppage_usesElapsedTime() {
        assertTrue(LiveMinuteLabelResolver.isLikelyFirstHalfStoppage(48, 50));
        assertFalse(LiveMinuteLabelResolver.isLikelyFirstHalfStoppage(48, 70));
    }

    @Test
    void isSupportedLeagueCode() {
        assertTrue(LiveMinuteLabelResolver.isSupportedLeagueCode("EPL"));
        assertFalse(LiveMinuteLabelResolver.isSupportedLeagueCode("UNKNOWN"));
    }

    @Test
    void stoppageLabelForBaseMinute() {
        assertEquals("45+", LiveMinuteLabelResolver.stoppageLabelForBaseMinute(45));
        assertEquals("90+", LiveMinuteLabelResolver.stoppageLabelForBaseMinute(90));
        assertEquals("105+", LiveMinuteLabelResolver.stoppageLabelForBaseMinute(105));
        assertEquals("120+", LiveMinuteLabelResolver.stoppageLabelForBaseMinute(120));
    }
}
