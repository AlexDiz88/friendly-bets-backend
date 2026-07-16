package net.friendly_bets.fifa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FifaMatchParserTest {

    @Test
    void mapsKnockoutStageNames() {
        assertEquals("round_of_32", FifaMatchParser.mapKnockoutStage("Round of 32"));
        assertEquals("round_of_16", FifaMatchParser.mapKnockoutStage("Round of 16"));
        assertEquals("quarter_final", FifaMatchParser.mapKnockoutStage("Quarter-final"));
        assertEquals("semi_final", FifaMatchParser.mapKnockoutStage("Semi-final"));
        assertEquals("third_place", FifaMatchParser.mapKnockoutStage("Play-off for third place"));
        assertEquals("third_place", FifaMatchParser.mapKnockoutStage("Bronze final"));
        assertEquals("final", FifaMatchParser.mapKnockoutStage("Final"));
        assertNull(FifaMatchParser.mapKnockoutStage("First Stage"));
    }
}
