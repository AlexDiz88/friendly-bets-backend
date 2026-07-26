package net.friendly_bets.soccer365;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchTeamStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Soccer365GameParserTest {

    private final Soccer365GameParser parser = new Soccer365GameParser();

    @Test
    @DisplayName("parses FT/HT/OT/pen, goals and full-match stats from game card")
    void parsesPenaltyShootoutCard() throws Exception {
        Path fixture = Path.of("src/test/resources/soccer365/game-2480009.html");
        String html = Files.readString(fixture, StandardCharsets.UTF_8);

        Soccer365ParsedFullMatch parsed = parser.parse(html);

        GameScore score = parsed.getGameScore();
        assertNotNull(score);
        assertEquals("1:1", score.getFullTime());
        assertEquals("1:0", score.getFirstTime());
        assertEquals("2:2", score.getOverTime());
        assertEquals("3:4", score.getPenalty());

        assertTrue(parsed.getGoals().stream().anyMatch(g ->
                "HOME".equals(g.getTeamSide())
                        && "20".equals(g.getMinute())
                        && !Boolean.TRUE.equals(g.getPenaltyShootout())));
        assertTrue(parsed.getGoals().stream().anyMatch(g -> Boolean.TRUE.equals(g.getPenaltyShootout())));
        assertTrue(parsed.getGoals().stream().anyMatch(g ->
                Boolean.TRUE.equals(g.getPenaltyShootout()) && Boolean.TRUE.equals(g.getMissed())));
        assertEquals("Завершен", parsed.getStatusText());

        MatchTeamStats stats = parsed.getStats();
        assertNotNull(stats);
        assertEquals(8, stats.getShotsHome());
        assertEquals(14, stats.getShotsAway());
        assertEquals(6, stats.getShotsOnTargetHome());
        assertEquals(8, stats.getShotsOnTargetAway());
        assertEquals(56, stats.getPossessionHome());
        assertEquals(44, stats.getPossessionAway());
        assertEquals(0, stats.getYellowCardsHome());
        assertEquals(4, stats.getYellowCardsAway());
    }

    @Test
    @DisplayName("Santos match: pengoal+owngoal, board FT without fake OT, shots/xG, team names")
    void parsesSantosChapecoenseCard() throws Exception {
        Path fixture = Path.of("src/test/resources/soccer365/game-2386678.html");
        String html = Files.readString(fixture, StandardCharsets.UTF_8);

        Soccer365ParsedFullMatch parsed = parser.parse(html);

        assertEquals("Сантос", parsed.getHomeTeamName());
        assertEquals("Шапекоэнсе", parsed.getAwayTeamName());
        assertTrue(parsed.getCompetitionName() != null && parsed.getCompetitionName().contains("Серия"));

        GameScore score = parsed.getGameScore();
        assertNotNull(score);
        // Score from goal events only: 36H + 51A + 62 OG (credited AWAY) + 89H pen = 2:2
        assertEquals("2:2", score.getFullTime());
        assertEquals("1:0", score.getFirstTime());
        assertNull(score.getOverTime());
        assertNull(score.getPenalty());

        assertEquals(4, parsed.getGoals().size());
        assertTrue(parsed.getGoals().stream().anyMatch(g ->
                "89".equals(g.getMinute())
                        && "HOME".equals(g.getTeamSide())
                        && Boolean.TRUE.equals(g.getPenalty())
                        && !Boolean.TRUE.equals(g.getPenaltyShootout())));
        assertTrue(parsed.getGoals().stream().anyMatch(g ->
                Boolean.TRUE.equals(g.getOwnGoal()) && "AWAY".equals(g.getTeamSide())));

        MatchTeamStats stats = parsed.getStats();
        assertNotNull(stats);
        assertEquals(21, stats.getShotsHome());
        assertEquals(5, stats.getShotsAway());
        assertEquals(7, stats.getShotsOnTargetHome());
        assertEquals(3, stats.getShotsOnTargetAway());
        assertEquals(64, stats.getPossessionHome());
        assertEquals(36, stats.getPossessionAway());
        assertEquals(3, stats.getYellowCardsHome());
        assertEquals(4, stats.getYellowCardsAway());
        assertEquals(1.74, stats.getXgHome(), 0.001);
        assertEquals(0.86, stats.getXgAway(), 0.001);
        assertFalse(parsed.getStatusText() == null || parsed.getStatusText().isBlank());
        assertEquals("Завершен", parsed.getStatusText());
    }
}
