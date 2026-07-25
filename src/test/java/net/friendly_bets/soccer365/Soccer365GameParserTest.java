package net.friendly_bets.soccer365;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchTeamStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
