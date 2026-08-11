package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlashscoreScoreParserTest {

    @Test
    void firstHalfScoreUsesSummaryIgIhNotResultBcBd() throws IOException {
        String summary = readFixture("flashscore/match-nurnberg-sui.feed");
        String result = readFixture("flashscore/match-nurnberg-sur.feed");

        assertEquals("2:0", FlashscoreMatchDetailParser.parseFirstHalfScoreFromSummary(summary));
        assertEquals("3:0", FlashscoreMatchDetailParser.parseFullTimeFromResult(
                Map.of("BC", "1", "BD", "0", "BA", "2", "BB", "0")));
        assertNull(FlashscoreMatchDetailParser.parseFirstHalfScoreFromSummary(result));
    }

    @Test
    void fullTimeFromResultFeedSumsHalfSegments() {
        assertEquals("3:0", FlashscoreMatchDetailParser.parseFullTimeFromResult(
                Map.of("BC", "1", "BD", "0", "BA", "2", "BB", "0")));
        assertEquals("3:1", FlashscoreMatchDetailParser.parseFullTimeFromResult(
                Map.of("AG", "3", "AH", "1")));
    }

    private static String readFixture(String path) throws IOException {
        try (var in = FlashscoreScoreParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
