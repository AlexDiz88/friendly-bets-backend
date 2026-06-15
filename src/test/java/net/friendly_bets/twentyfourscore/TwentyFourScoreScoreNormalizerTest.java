package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TwentyFourScoreScoreNormalizerTest {

    private final TwentyFourScoreScoreNormalizer normalizer = new TwentyFourScoreScoreNormalizer();

    @Test
    void normalizesFinishedMatch() {
        TwentyFourScoreListMatch listMatch = TwentyFourScoreListMatch.builder()
                .statusText("Завершен")
                .fullTimeScore("5:1")
                .firstHalfScore("2:1")
                .build();
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = normalizer.normalize(listMatch);
        assertNotNull(normalized);
        assertEquals("FINISHED", normalized.status());
        assertEquals("5:1", normalized.gameScore().getFullTime());
    @Test
    void normalizesLiveMatchFromDailyList() {
        TwentyFourScoreListMatch listMatch = TwentyFourScoreListMatch.builder()
                .statusText("Идёт 50'")
                .liveMinuteLabel("50'")
                .fullTimeScore("0:1")
                .firstHalfScore("0:1")
                .build();
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = normalizer.normalize(listMatch);
        assertNotNull(normalized);
        assertEquals("IN_PLAY", normalized.status());
        assertEquals("50'", normalized.liveMinuteLabel());
    }
}
