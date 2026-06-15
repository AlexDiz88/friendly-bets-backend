package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TwentyFourScoreMatchPageParserTest {

    private final TwentyFourScoreMatchPageParser parser = new TwentyFourScoreMatchPageParser();

    @Test
    void parseFinishedMatch() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/twentyfourscore/sweden-tunisia.html"));
        TwentyFourScoreMatchDetails details = parser.parse(html, "/football/match/849893-sweden-tunisia");
        assertNotNull(details);
        assertEquals(849893L, details.getExternalMatchId());
        assertEquals("Швеция", details.getHomeTeamName());
        assertEquals("Тунис", details.getAwayTeamName());
        assertEquals("5:1", details.getFullTimeScore());
        assertEquals("2:1", details.getFirstHalfScore());
        assertEquals("Завершен", details.getStatusText());
        assertEquals(Integer.valueOf(1), details.getMatchday());
        assertNotNull(details.getKickoffAt());
    }
}
