package net.friendly_bets.footballdata;

import net.friendly_bets.models.GameScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExternalMatchScoreViewTest {

    @Test
    void liveWithoutScore_returnsDash() {
        GameScore score = GameScore.builder().firstTime("1:0").build();
        assertEquals("—", ExternalMatchScoreView.format(score, "IN_PLAY", false));
    }

    @Test
    void liveWithMinute_returnsFullTimeOnly() {
        GameScore score = GameScore.builder().fullTime("1:1").firstTime("0:1").build();
        assertEquals("1:1", ExternalMatchScoreView.format(score, "IN_PLAY", false, "67'"));
    }

    @Test
    void liveWithScore_returnsFullTimeOnly() {
        GameScore score = GameScore.builder().fullTime("1:1").firstTime("0:1").build();
        assertEquals("1:1", ExternalMatchScoreView.format(score, "IN_PLAY", false, null));
    }

    @Test
    void finishedWithHalftime_returnsCombinedView() {
        GameScore score = GameScore.builder().fullTime("2:0").firstTime("1:0").build();
        assertEquals("2:0 (1:0)", ExternalMatchScoreView.format(score, "FINISHED", true));
    }
}
