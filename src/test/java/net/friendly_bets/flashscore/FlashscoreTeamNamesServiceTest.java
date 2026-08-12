package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreTeamNamesServiceTest {

    @Test
    void parseTeamNamesFromTournamentFeedScopedByStageId() {
        String html = """
                <script>
                data: `SA÷1¬~ZA÷ENGLAND: Premier League¬ZC÷CfoA8Dmm¬ZL÷/football/england/premier-league/¬~AA÷x¬FH÷Arsenal¬FK÷Chelsea¬~AA÷y¬FH÷Liverpool¬FK÷Everton¬`
                data: `SA÷1¬~ZA÷BHUTAN: Premier League¬ZC÷OtherStage¬~AA÷z¬FH÷Ugyen Academy¬FK÷BFF Academy U19¬`
                </script>
                """;
        var names = FlashscoreTeamNamesService.parseTeamNamesFromTournamentHtml(html, "CfoA8Dmm");
        assertTrue(names.contains("Arsenal"));
        assertTrue(names.contains("Chelsea"));
        assertTrue(names.contains("Liverpool"));
        assertTrue(names.contains("Everton"));
        assertFalse(names.contains("Ugyen Academy"));
        assertFalse(names.contains("BFF Academy U19"));
        assertEquals(4, names.size());
    }

    @Test
    void parseTeamNamesFromStandingsRowsWhenPresent() {
        String html = """
                data: `SA÷1¬~ZA÷GERMANY: Bundesliga¬ZC÷jg0MwVuC¬~TI÷x¬TN÷Bayern Munich¬~TI÷y¬TN÷Dortmund¬`
                """;
        var names = FlashscoreTeamNamesService.parseTeamNamesFromTournamentHtml(html, "jg0MwVuC");
        assertEquals(2, names.size());
        assertTrue(names.contains("Bayern Munich"));
        assertTrue(names.contains("Dortmund"));
    }

    @Test
    void extractTeamNamesFromFeedBlockReturnsOnlyRequestedStage() {
        String block = """
                SA÷1¬~ZA÷ENGLAND: Premier League¬ZC÷CfoA8Dmm¬~AA÷x¬FH÷Arsenal¬FK÷Chelsea¬~ZA÷OTHER¬ZC÷OtherStage¬~AA÷y¬FH÷Ugyen Academy¬FK÷Ararat¬
                """;
        Set<String> names = FlashscoreTeamNamesService.extractTeamNamesFromFeedBlock(block, "CfoA8Dmm");
        assertEquals(Set.of("Arsenal", "Chelsea"), names);
    }
}
