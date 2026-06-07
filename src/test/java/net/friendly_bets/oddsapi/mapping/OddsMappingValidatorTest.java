package net.friendly_bets.oddsapi.mapping;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsMappingValidatorTest {

    @Test
    void highOddsWithLargeAbsoluteGapButModerateRelativeSpreadIsNotMismatch() {
        assertFalse(OddsMappingValidator.isCrossBookmakerMismatch(Map.of(
                "Bet365", "8.00",
                "1xbet", "12.00"
        )));
    }

    @Test
    void largeRelativeSpreadIsMismatch() {
        assertTrue(OddsMappingValidator.isCrossBookmakerMismatch(Map.of(
                "Bet365", "1.15",
                "1xbet", "4.65"
        )));
    }

    @Test
    void singleBookmakerIsNotMismatch() {
        assertFalse(OddsMappingValidator.isCrossBookmakerMismatch(Map.of(
                "Bet365", "3.50"
        )));
    }
}
