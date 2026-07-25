package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TwentyFourScoreStandingsParserTest {

    private final TwentyFourScoreStandingsParser parser = new TwentyFourScoreStandingsParser();

    @Test
    @DisplayName("extractDataKey reads AJAX key from standings shell")
    void extractDataKey_fromShell() {
        String html = """
                $.ajax({
                			type: "GET",
                			url: "/backend/load_page_data.php",
                			data: {"data_key" : "abc123KEY"},
                			dataType: "html"
                		})
                """;
        assertEquals("abc123KEY", parser.extractDataKey(html));
        assertNull(parser.extractDataKey("<html></html>"));
    }

    @Test
    @DisplayName("parseTeamNames keeps unique standings team link texts")
    void parseTeamNames_uniqueFromLinks() {
        String html = """
                <table>
                  <tr><th class="team left">Команда</th></tr>
                  <tr><td><a href="/football/team/england/arsenal/">Арсенал</a></td></tr>
                  <tr><td><a href="/football/team/england/liverpool/">Ливерпуль</a></td></tr>
                  <tr><td><a href="/football/team/england/arsenal/">Арсенал</a></td></tr>
                </table>
                """;
        assertEquals(List.of("Арсенал", "Ливерпуль"), parser.parseTeamNames(html));
    }
}
