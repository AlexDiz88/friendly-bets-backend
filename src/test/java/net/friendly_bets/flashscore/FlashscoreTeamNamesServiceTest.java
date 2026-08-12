package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreTeamNamesServiceTest {

    @Test
    void parseTeamNamesFromEmbeddedFeedFields() {
        String html = """
                data: `SA÷1¬~AA÷x¬CX÷Арсенал¬AF÷Челси¬~AA÷y¬AE÷Ливерпуль¬FK÷Everton¬FH÷Arsenal¬FK÷Chelsea¬`
                """;
        var names = FlashscoreTeamNamesService.parseTeamNamesFromTournamentHtml(html);
        assertTrue(names.contains("Arsenal"));
        assertTrue(names.contains("Chelsea"));
        assertTrue(names.contains("Everton"));
        assertFalse(names.contains("Арсенал"));
        assertEquals(3, names.size());
    }
}
