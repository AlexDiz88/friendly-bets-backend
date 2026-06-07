package net.friendly_bets.oddsapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsHandicapLineTest {

    @Test
    void xbetSpreadAwayInvertsApiHdpSign() {
        assertEquals(-2.5, OddsHandicapLine.effectiveLine("-2.5", true));
        assertEquals(2.5, OddsHandicapLine.effectiveLine("-2.5", false));
        assertEquals(1.5, OddsHandicapLine.effectiveLine("-1.5", false));
        assertEquals(-1.5, OddsHandicapLine.effectiveLine("+1.5", false));
        assertEquals(-1.0, OddsHandicapLine.effectiveLine("+1", false));
    }

    @Test
    void bet365AwayInvertsApiHdpSign() {
        assertEquals(-1.5, OddsHandicapLine.effectiveLine("-1.5", true, OddsHandicapLine.INVERT_AWAY_SIGN_BET365));
        assertEquals(1.5, OddsHandicapLine.effectiveLine("-1.5", false, OddsHandicapLine.INVERT_AWAY_SIGN_BET365));
        assertEquals(1.0, OddsHandicapLine.effectiveLine("-1", false, OddsHandicapLine.INVERT_AWAY_SIGN_BET365));
    }

    @Test
    void formatsSignedLine() {
        assertEquals("-2.5", OddsHandicapLine.formatSigned(-2.5));
        assertEquals("+1.5", OddsHandicapLine.formatSigned(1.5));
        assertEquals("0", OddsHandicapLine.formatSigned(0));
    }

}
