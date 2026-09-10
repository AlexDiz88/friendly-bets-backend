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
    void parseCalendar_readsCalendarListByRoundNumber_andDatetimeKickoff() {
        String html = """
                <div class="calendar-list" data-testid="calendar-list">
                <div class="calendar-group-header calendar-group-header--stage">3 тур</div>
                <div class="calendar-group-header calendar-group-header--day">11 сентября</div>
                <article class="calendar-card" data-match-status="NOT_STARTED">
                  <a href="/football/match/schalke-vs-union-berlin/" title="Матч Унион Берлин - Шальке"
                     class="calendar-card__match-link"></a>
                  <time class="calendar-status__moment" datetime="2026-09-11T18:30:00Z">21:30</time>
                  <a class="calendar-team calendar-card__home" href="/football/club/union-berlin/"
                     title="Перейти в профиль команды Унион Берлин" data-testid="calendar-team-name">
                    Унион Берлин <img alt="Логотип команды Унион Берлин"/>
                  </a>
                  <div class="calendar-score calendar-score__score--empty calendar-card__score">
                    <span class="calendar-score__score">-</span>
                    <div class="calendar-score__separator">-</div>
                    <span class="calendar-score__score">-</span>
                  </div>
                  <a class="calendar-team calendar-team--away calendar-card__away" href="/football/club/schalke/"
                     title="Перейти в профиль команды Шальке" data-testid="calendar-team-name">
                    Шальке <img alt="Логотип команды Шальке"/>
                  </a>
                </article>
                <div class="calendar-group-header calendar-group-header--stage">4 тур</div>
                <div class="calendar-group-header calendar-group-header--day">19 сентября</div>
                <article class="calendar-card" data-match-status="FINISHED">
                  <a href="/football/match/bayern-vs-dortmund/" class="calendar-card__match-link"></a>
                  <time class="calendar-status__moment" datetime="2026-09-19T13:30:00Z">16:30</time>
                  <a class="calendar-team calendar-card__home" title="Перейти в профиль команды Бавария">Бавария</a>
                  <div class="calendar-card__score"><span>2</span><span>-</span><span>1</span></div>
                  <a class="calendar-team calendar-card__away" title="Перейти в профиль команды Боруссия Д">Боруссия Д</a>
                </article>
                <div class="calendar-group-header calendar-group-header--stage">плей-офф</div>
                <article class="calendar-card" data-match-status="NOT_STARTED">
                  <a href="/football/match/ignored-vs-playoff/" class="calendar-card__match-link"></a>
                  <a class="calendar-team calendar-card__home" title="Перейти в профиль команды А">А</a>
                  <a class="calendar-team calendar-card__away" title="Перейти в профиль команды Б">Б</a>
                </article>
                </div>
                """;

        SportsRuParsedSchedule parsed = parser.parseCalendar(html);
        assertEquals(2, parsed.getRounds().size());

        SportsRuParsedSchedule.Round md3 = parsed.getRounds().get(0);
        assertEquals(3, md3.getNumber());
        assertEquals(1, md3.getMatches().size());
        SportsRuParsedSchedule.Match m3 = md3.getMatches().get(0);
        // home/away from card classes — not from URL slug (schalke-vs-union-berlin)
        assertEquals("Унион Берлин", m3.getHomeName());
        assertEquals("Шальке", m3.getAwayName());
        assertEquals("/football/match/schalke-vs-union-berlin/", m3.getMatchPath());
        assertEquals("SCHEDULED", m3.getStatus());
        assertEquals(Instant.parse("2026-09-11T18:30:00Z"), m3.getUtcKickoff());

        SportsRuParsedSchedule.Round md4 = parsed.getRounds().get(1);
        assertEquals(4, md4.getNumber());
        SportsRuParsedSchedule.Match m4 = md4.getMatches().get(0);
        assertEquals("Бавария", m4.getHomeName());
        assertEquals("Боруссия Д", m4.getAwayName());
        assertEquals("FINISHED", m4.getStatus());
        assertEquals(Instant.parse("2026-09-19T13:30:00Z"), m4.getUtcKickoff());
    }

    @Test
    void parseCalendar_prefersCalendarListOverLegacyWhenBothPresent() {
        String html = """
                <div class="calendar-list">
                <div class="calendar-group-header--stage">2 тур</div>
                <article class="calendar-card" data-match-status="NOT_STARTED">
                  <a href="/football/match/a-vs-b/" class="calendar-card__match-link"></a>
                  <time class="calendar-status__moment" datetime="2026-09-12T14:00:00Z"></time>
                  <a class="calendar-team calendar-card__home" title="Перейти в профиль команды Хозяева">Хозяева</a>
                  <a class="calendar-team calendar-card__away" title="Перейти в профиль команды Гости">Гости</a>
                </article>
                </div>
                <h3>1 тур</h3>
                <table class="stat-table"><tbody>
                <tr>
                <td class="owner-td"><a class="player" title="ЛегасиХ">ЛегасиХ</a></td>
                <td class="score-td"><a class="score" href="/football/match/legacy-h-vs-a/"><b>- : -</b></a></td>
                <td class="guests-td"><a class="player" title="ЛегасиГ">ЛегасиГ</a></td>
                </tr>
                </tbody></table>
                """;
        SportsRuParsedSchedule parsed = parser.parseCalendar(html);
        assertEquals(1, parsed.getRounds().size());
        assertEquals(2, parsed.getRounds().get(0).getNumber());
        assertEquals("Хозяева", parsed.getRounds().get(0).getMatches().get(0).getHomeName());
    }

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

    @Test
    void parseVueCalendar_readsMultipleToursInOneColumn_andTiteTypo() {
        String html = """
                <div class="match-schedule-column match-schedule__columns-item--hide">
                <div class="match-schedule-column__group-header">плей-офф</div>
                <div class="match-teaser match-schedule-column__matches-item">
                <a class="match-teaser__link" href="/football/match/qual-a-vs-qual-b/">
                <div class="match-teaser__team match-teaser__team--home">
                <span class="match-teaser__team-name" title="КуПС">КуПС</span></div>
                <div class="match-teaser__team-score"><span>-</span><span>–</span><span>-</span></div>
                <div class="match-teaser__team match-teaser__team--away">
                <span class="match-teaser__team-name" title="Сабах">Сабах</span></div>
                </a>
                </div>
                </div>
                <div class="match-schedule-column">
                <div class="match-schedule-column__group-header">1 тур</div>
                <div class="match-teaser match-schedule-column__matches-item">
                <a class="match-teaser__link" href="https://www.sports.ru/football/match/aek-athens-vs-lask/">
                <div class="match-teaser__team match-teaser__team--home">
                <span class="match-teaser__team-name" tite="АЕК">АЕК</span></div>
                <div class="match-teaser__team-score"><span>-</span><span>–</span><span>-</span></div>
                <div class="match-teaser__team match-teaser__team--away">
                <span class="match-teaser__team-name" title="ЛАСК">ЛАСК</span></div>
                </a>
                </div>
                <div class="match-schedule-column__group-header">2 тур</div>
                <div class="match-teaser match-schedule-column__matches-item">
                <a class="match-teaser__link" href="/football/match/inter-vs-brugge-fc/">
                <div class="match-teaser__team match-teaser__team--home">
                <span class="match-teaser__team-name" title="Интер">Интер</span></div>
                <div class="match-teaser__team-score"><span>1</span><span>–</span><span>0</span></div>
                <div class="match-teaser__team match-teaser__team--away">
                <span class="match-teaser__team-name" title="Брюгге">Брюгге</span></div>
                </a>
                </div>
                </div>
                """;

        SportsRuParsedSchedule parsed = parser.parseCalendar(html);
        assertEquals(2, parsed.getRounds().size());
        assertEquals(1, parsed.getRounds().get(0).getNumber());
        assertEquals(1, parsed.getRounds().get(0).getMatches().size());
        SportsRuParsedSchedule.Match md1 = parsed.getRounds().get(0).getMatches().get(0);
        assertEquals("АЕК", md1.getHomeName());
        assertEquals("ЛАСК", md1.getAwayName());
        assertEquals("/football/match/aek-athens-vs-lask/", md1.getMatchPath());
        assertEquals("SCHEDULED", md1.getStatus());

        assertEquals(2, parsed.getRounds().get(1).getNumber());
        SportsRuParsedSchedule.Match md2 = parsed.getRounds().get(1).getMatches().get(0);
        assertEquals("Интер", md2.getHomeName());
        assertEquals("FINISHED", md2.getStatus());

        var names = parser.parseTeamNamesFromMatchday(html, 1);
        assertEquals(2, names.size());
        assertTrue(names.contains("АЕК"));
        assertTrue(names.contains("ЛАСК"));
    }

    @Test
    void parseTeamNamesFromTablePage() {
        String html = """
                <table class="stat-table table sortable-table">
                <tbody>
                <tr>
                <td class="name-td"><a class="name" href="https://www.sports.ru/football/club/milan/" title="Милан">Милан</a></td>
                </tr>
                <tr>
                <td class="name-td"><a class="name" href="https://www.sports.ru/football/club/juventus/" title="Ювентус">Ювентус</a></td>
                </tr>
                </tbody>
                </table>
                """;
        var names = parser.parseTeamNamesFromTable(html);
        assertEquals(2, names.size());
        assertTrue(names.contains("Милан"));
        assertTrue(names.contains("Ювентус"));
    }

    @Test
    void parseTeamNamesFromJsonLd_competitorList() {
        String html = """
                <html><head>
                <script type="application/ld+json">{"@context":"https://schema.org","@type":"SportsEvent",
                "competitor":[
                {"@type":"SportsTeam","name":"Арсенал"},
                {"@type":"SportsTeam","name":"Ливерпуль"}
                ]}</script>
                </head><body></body></html>
                """;
        var names = parser.parseTeamNamesFromJsonLd(html);
        assertEquals(2, names.size());
        assertTrue(names.contains("Арсенал"));
        assertTrue(names.contains("Ливерпуль"));
    }
}
