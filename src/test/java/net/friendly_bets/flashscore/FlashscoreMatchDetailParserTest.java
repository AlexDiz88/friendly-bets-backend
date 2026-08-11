package net.friendly_bets.flashscore;

import net.friendly_bets.providers.FullMatchStatusSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreMatchDetailParserTest {

    private final FlashscoreMatchDetailParser parser = new FlashscoreMatchDetailParser();

    @Test
    void parsesFinishedMatchGoalsStatsAndAddedTime() throws IOException {
        String summary = readFixture("flashscore/match-nurnberg-sui.feed");
        String stats = readFixture("flashscore/match-nurnberg-st.feed");
        String result = readFixture("flashscore/match-nurnberg-sur.feed");

        FlashscoreParsedFullMatch parsed = parser.parse(summary, stats, result, "nNUWaFf5");

        assertTrue(FullMatchStatusSupport.isProviderFinished(parsed.getStatusText()));
        assertNotNull(parsed.getGameScore());
        assertEquals("3:0", parsed.getGameScore().getFullTime());
        assertEquals("1:0", parsed.getGameScore().getFirstTime());
        assertEquals(3, parsed.getGoals().size());
        assertTrue(parsed.getGoals().stream().allMatch(g -> g.getRedCard() == null || !g.getRedCard()));

        assertNotNull(parsed.getStats());
        assertEquals(56, parsed.getStats().getPossessionHome());
        assertEquals(44, parsed.getStats().getPossessionAway());
        assertEquals(16, parsed.getStats().getShotsHome());
        assertEquals(10, parsed.getStats().getShotsAway());
        assertEquals(7, parsed.getStats().getShotsOnTargetHome());
        assertEquals(1, parsed.getStats().getShotsOnTargetAway());
        assertEquals(0, parsed.getStats().getYellowCardsHome());
        assertEquals(2, parsed.getStats().getYellowCardsAway());
        assertEquals(2.24, parsed.getStats().getXgHome());
        assertEquals(1.96, parsed.getStats().getXgAway());

        assertEquals(3, parsed.getAddedTimeFirstHalf());
    }

    private static String readFixture(String path) throws IOException {
        try (var in = FlashscoreMatchDetailParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
