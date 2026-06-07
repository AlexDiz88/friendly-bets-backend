package net.friendly_bets.oddsapi;

import net.friendly_bets.models.enums.BetTitleCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsCorrectScoreUtilsTest {

    @Test
    void parsesHyphenAndColonScores() {
        assertNotNull(OddsCorrectScoreUtils.parseScore("1-0"));
        assertNotNull(OddsCorrectScoreUtils.parseScore("2:1"));
    }

    @Test
    void selectionCodeForBetTitle() {
        assertEquals("0-0", OddsCorrectScoreUtils.selectionCodeForBetTitle(BetTitleCode.GAME_SCORE_0_0));
        assertEquals("3-1", OddsCorrectScoreUtils.selectionCodeForBetTitle(BetTitleCode.GAME_SCORE_3_1));
    }

    @Test
    void sortsScoresInExpectedOrder() {
        assertTrueOrder("0-0", "1-0");
        assertTrueOrder("1-0", "0-1");
        assertTrueOrder("0-1", "1-1");
        assertTrueOrder("1-1", "2-0");
    }

    private static void assertTrueOrder(String first, String second) {
        assertTrue(
                OddsCorrectScoreUtils.sortKey(first) < OddsCorrectScoreUtils.sortKey(second),
                first + " should precede " + second
        );
    }
}
