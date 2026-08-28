package net.friendly_bets.eurofootball;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EuroFootballTeamNamesServiceTest {

    @Test
    void parseTeamNames_usesLeagueWidgetNotSiteDropdown() throws Exception {
        String html;
        try (var in = Objects.requireNonNull(
                getClass().getResourceAsStream("/eurofootball/league-tables-widget.html"))) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(List.of("Арсенал", "Ливерпуль"), EuroFootballTeamNamesService.parseTeamNamesFromTablesHtml(html));
    }

    @Test
    void parseTeamNames_emptyWhenOnlySiteWideDropdown() {
        String html = """
                <div class="block">
                  <div class="block__content block__select-turnir-tables">
                    <div class="tournament-tables-widget">
                      <table class="table table-turnir">
                        <tr><td><a href="/team/spartak">Спартак Москва</a></td></tr>
                      </table>
                    </div>
                  </div>
                </div>
                """;
        assertEquals(List.of(), EuroFootballTeamNamesService.parseTeamNamesFromTablesHtml(html));
    }
}
