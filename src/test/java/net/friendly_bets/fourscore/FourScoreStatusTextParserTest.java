package net.friendly_bets.fourscore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FourScoreStatusTextParserTest {

    @Test
    void parsesLiveMinuteFromStatusText() {
        FourScoreStatusTextParser.ParsedStatus parsed = FourScoreStatusTextParser.parse("Идёт 72'");
        assertEquals("IN_PLAY", parsed.mappedStatus());
        assertEquals("72'", parsed.liveMinuteLabel());
    }

    @Test
    void parsesStoppageTimeMinute() {
        FourScoreStatusTextParser.ParsedStatus parsed = FourScoreStatusTextParser.parse("Идёт 90+2'");
        assertEquals("IN_PLAY", parsed.mappedStatus());
        assertEquals("90+2'", parsed.liveMinuteLabel());
    }

    @Test
    void needsEventDetailsForLiveScoreWithoutStatus() {
        assertTrue(FourScoreStatusTextParser.needsEventDetails(null, 0, 1));
        assertFalse(FourScoreStatusTextParser.needsEventDetails(null, 0, 0));
        assertFalse(FourScoreStatusTextParser.needsEventDetails("Не началось", 0, 0));
    }
}
