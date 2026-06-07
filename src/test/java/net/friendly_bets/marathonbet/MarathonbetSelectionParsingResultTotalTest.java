package net.friendly_bets.marathonbet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarathonbetSelectionParsingResultTotalTest {

    @Test
    void parsesResultTotalLineFromMarketName() {
        assertEquals(2.5, MarathonbetSelectionParsing.parseResultTotalLine(
                "Результат матча + тотал голов 2.5"));
        assertEquals(1.5, MarathonbetSelectionParsing.parseResultTotalLine(
                "Результат матча + тотал голов 1,5"));
        assertNull(MarathonbetSelectionParsing.parseResultTotalLine("Результат"));
    }
}
