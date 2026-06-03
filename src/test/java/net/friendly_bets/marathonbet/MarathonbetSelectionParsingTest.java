package net.friendly_bets.marathonbet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetSelectionParsingTest {

    @Test
    void parsesHandicapLine() {
        assertEquals(-1.0, MarathonbetSelectionParsing.parseHandicapLine("Мексика (-1)"));
        assertEquals(1.0, MarathonbetSelectionParsing.parseHandicapLine("ЮАР (+1)"));
    }

    @Test
    void parsesTotalLine() {
        assertEquals(2.5, MarathonbetSelectionParsing.parseTotalLine("Больше 2.5"));
        assertTrue(MarathonbetSelectionParsing.isTotalOver("Больше 2.5"));
    }

    @Test
    void parsesCorrectScoreFromTeamPrefix() {
        int[] score = MarathonbetSelectionParsing.parseCorrectScoreHomeAway(
                "Мексика 2 - 1", "Мексика", "ЮАР");
        assertArrayEquals(new int[] { 2, 1 }, score);

        int[] awayWin = MarathonbetSelectionParsing.parseCorrectScoreHomeAway(
                "ЮАР 2 - 1", "Мексика", "ЮАР");
        assertArrayEquals(new int[] { 1, 2 }, awayWin);

        int[] draw = MarathonbetSelectionParsing.parseCorrectScoreHomeAway(
                "Ничья 0 - 0", "Мексика", "ЮАР");
        assertArrayEquals(new int[] { 0, 0 }, draw);
    }
}
