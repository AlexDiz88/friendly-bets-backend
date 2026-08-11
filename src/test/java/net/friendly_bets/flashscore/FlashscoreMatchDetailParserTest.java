package net.friendly_bets.flashscore;

import net.friendly_bets.providers.FullMatchStatusSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(4, parsed.getGoals().size());
        assertTrue(parsed.getGoals().stream().anyMatch(
                g -> Boolean.TRUE.equals(g.getMissed()) && "Zoma M. A.".equals(g.getPlayerName())));
        assertEquals(3, parsed.getGoals().stream()
                .filter(g -> !Boolean.TRUE.equals(g.getMissed()) && !Boolean.TRUE.equals(g.getRedCard()))
                .count());
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

        assertNull(parsed.getAddedTimeFirstHalf());
        assertNull(parsed.getAddedTimeSecondHalf());
    }

    @Test
    void parsesOwnGoalAndTeamNamesFromH2H() throws IOException {
        String summary = readFixture("flashscore/match-horsens-sui.feed");
        String result = readFixture("flashscore/match-horsens-sur.feed");
        String h2h = readFixture("flashscore/match-horsens-hh.feed");

        FlashscoreParsedFullMatch parsed = parser.parse(summary, null, result, "MuXsRLdK", h2h);

        assertEquals("Horsens", parsed.getHomeTeamName());
        assertEquals("Brondby", parsed.getAwayTeamName());
        assertEquals("Superliga", parsed.getCompetitionName());
        assertEquals("0:2", parsed.getGameScore().getFullTime());
        assertEquals(2, parsed.getGoals().stream()
                .filter(g -> !Boolean.TRUE.equals(g.getMissed()) && !Boolean.TRUE.equals(g.getRedCard()))
                .count());
        assertTrue(parsed.getGoals().stream().anyMatch(
                g -> Boolean.TRUE.equals(g.getOwnGoal())
                        && "Kupijbida M.".equals(g.getPlayerName())
                        && "AWAY".equals(g.getTeamSide())));
    }

    @Test
    void parsesPenaltyGoalAndVarDisallowedGoal() throws IOException {
        String summary = readFixture("flashscore/match-charleroi-sui.feed");
        String result = readFixture("flashscore/match-charleroi-sur.feed");
        String h2h = readFixture("flashscore/match-charleroi-hh.feed");

        FlashscoreParsedFullMatch parsed = parser.parse(summary, null, result, "2a2t8EQF", h2h);

        assertEquals("3:1", parsed.getGameScore().getFullTime());
        assertEquals("2:0", parsed.getGameScore().getFirstTime());
        assertTrue(parsed.getGoals().stream().anyMatch(
                g -> Boolean.TRUE.equals(g.getPenalty())
                        && "Guiagon P.".equals(g.getPlayerName())
                        && "24".equals(g.getMinute())));
        assertTrue(parsed.getGoals().stream().anyMatch(
                g -> Boolean.TRUE.equals(g.getVarDisallowed())
                        && "Keita C.".equals(g.getPlayerName())
                        && "47".equals(g.getMinute())));
        assertEquals(4, parsed.getGoals().stream()
                .filter(g -> !Boolean.TRUE.equals(g.getMissed())
                        && !Boolean.TRUE.equals(g.getRedCard())
                        && !Boolean.TRUE.equals(g.getVarDisallowed()))
                .count());
    }

    private static String readFixture(String path) throws IOException {
        try (var in = FlashscoreMatchDetailParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
