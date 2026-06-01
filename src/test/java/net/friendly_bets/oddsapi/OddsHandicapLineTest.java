package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsHandicapLineTest {

    @Test
    void awayGetsOppositeSignOfApiHdp() {
        assertEquals(-2.5, OddsHandicapLine.effectiveLine("-2.5", true));
        assertEquals(2.5, OddsHandicapLine.effectiveLine("-2.5", false));
        assertEquals(1.5, OddsHandicapLine.effectiveLine("-1.5", false));
        assertEquals(-1.5, OddsHandicapLine.effectiveLine("+1.5", false));
    }

    @Test
    void formatsSignedLine() {
        assertEquals("-2.5", OddsHandicapLine.formatSigned(-2.5));
        assertEquals("+1.5", OddsHandicapLine.formatSigned(1.5));
        assertEquals("0", OddsHandicapLine.formatSigned(0));
    }
}
