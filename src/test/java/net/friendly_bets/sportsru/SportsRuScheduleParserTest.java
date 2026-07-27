package net.friendly_bets.sportsru;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SportsRuScheduleParserTest {

    private final SportsRuScheduleParser parser = new SportsRuScheduleParser();

    @Test
    void parseCalendar_readsRoundAndTeams() {
        String html = """
                <html><body>
                <h3>1 тур</h3>
                <div class="stat mB15">
                <table class="stat-table">
                <tbody>
                <tr>
                <td class="name-td alLeft"><a href="/football/match/2026-08-21/">21.08.2026<span class="sp">|</span>22:00</a></td>
                <td class="owner-td"><a class="player" href="https://www.sports.ru/football/club/arsenal/" title="Арсенал">Арсенал</a></td>
                <td class="score-td"><a class="score" href="https://www.sports.ru/football/match/arsenal-vs-coventry-city-fc/"><b>- : -</b></a></td>
                <td class="guests-td"><a class="player" href="https://www.sports.ru/football/club/coventry-city-fc/" title="Ковентри">Ковентри</a></td>
                </tr>
                </tbody>
                </table>
                </div>
                <h3>2 тур</h3>
                <div class="stat mB15">
                <table class="stat-table"><tbody></tbody></table>
                </div>
                </body></html>
                """;

        SportsRuParsedSchedule parsed = parser.parseCalendar(html);
        assertEquals(2, parsed.getRounds().size());
        assertEquals(1, parsed.getRounds().get(0).getNumber());
        assertEquals(1, parsed.getRounds().get(0).getMatches().size());
        SportsRuParsedSchedule.Match match = parsed.getRounds().get(0).getMatches().get(0);
        assertEquals("Арсенал", match.getHomeName());
        assertEquals("Ковентри", match.getAwayName());
        assertEquals("/football/match/arsenal-vs-coventry-city-fc/", match.getMatchPath());
        assertEquals("SCHEDULED", match.getStatus());
        assertNull(match.getUtcKickoff());
    }

    @Test
    void parseUtcKickoff_prefersScheduledAtZ() {
        String html = """
                {"football-online-match":{"scheduledAt":"2026-08-21T19:00:00Z","matchStatus":"NOT_STARTED"},
                "microdata":"{\\"startDate\\":\\"2026-08-21T22:00:00+03:00\\"}"}
                """;
        Instant kickoff = parser.parseUtcKickoffFromMatchHtml(html);
        assertNotNull(kickoff);
        assertEquals(Instant.parse("2026-08-21T19:00:00Z"), kickoff);
    }

    @Test
    void parseUtcKickoff_acceptsPiniaObjectForm() {
        String html = "return {\"football-online-match\":{scheduledAt:\"2026-08-21T19:00:00Z\",matchStatus:\"NOT_STARTED\"}}";
        Instant kickoff = parser.parseUtcKickoffFromMatchHtml(html);
        assertEquals(Instant.parse("2026-08-21T19:00:00Z"), kickoff);
    }

    @Test
    void parseUtcKickoff_fallsBackToStartDateOffset() {
        String html = "{\"startDate\":\"2026-08-21T22:00:00+03:00\"}";
        Instant kickoff = parser.parseUtcKickoffFromMatchHtml(html);
        assertEquals(Instant.parse("2026-08-21T19:00:00Z"), kickoff);
    }

    @Test
    void parseTeamNamesFromMatchdayOne() {
        String html = """
                <h3>1 тур</h3>
                <div class="stat"><table class="stat-table"><tbody>
                <tr>
                <td class="owner-td"><a class="player" title="Арсенал">А</a></td>
                <td class="score-td"><a class="score" href="/football/match/a-vs-b/"><b>1 : 0</b></a></td>
                <td class="guests-td"><a class="player" title="Ковентри">К</a></td>
                </tr>
                </tbody></table></div>
                """;
        var names = parser.parseTeamNamesFromMatchday(html, 1);
        assertEquals(2, names.size());
        assertTrue(names.contains("Арсенал"));
        assertTrue(names.contains("Ковентри"));
    }
}
