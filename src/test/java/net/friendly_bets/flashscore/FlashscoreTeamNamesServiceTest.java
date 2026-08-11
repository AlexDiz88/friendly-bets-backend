package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreTeamNamesServiceTest {

    @Test
    void parseTeamNamesFromEmbeddedFeedFields() {
        String html = """
                data: `SA÷1¬~AA÷x¬CX÷Arsenal¬AF÷Chelsea¬~AA÷y¬AE÷Liverpool¬FK÷Everton¬`
                """;
        var names = FlashscoreTeamNamesService.parseTeamNamesFromTournamentHtml(html);
        assertTrue(names.contains("Arsenal"));
        assertTrue(names.contains("Chelsea"));
        assertTrue(names.contains("Liverpool"));
        assertTrue(names.contains("Everton"));
        assertEquals(4, names.size());
    }
}
