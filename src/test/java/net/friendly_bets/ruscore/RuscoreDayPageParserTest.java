package net.friendly_bets.ruscore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuscoreDayPageParserTest {

    private final RuscoreDayPageParser parser = new RuscoreDayPageParser();

    @Test
    void parsesMatchesKickoffLeagueHeadersAndSeasonIds() throws IOException {
        String html = readFixture("ruscore/day-mini.html");
        RuscoreParsedDayPage page = parser.parse(html, LocalDate.of(2026, 8, 9));
        assertNotNull(page.getCompetitions());
        assertEquals(3, page.getCompetitions().size());

        RuscoreParsedDayPage.CompetitionBlock secondBl = findCompetition(page, "Вторая Бундеслига 26/27");
        assertNotNull(secondBl);
        assertEquals(5457, secondBl.getSeasonId());
        assertEquals("germany-2-bundesliga", secondBl.getTournamentSlug());
        assertEquals(1, secondBl.getMatches().size());
        assertEquals("574496", secondBl.getMatches().get(0).getEventId());

        RuscoreParsedDayPage.Match dinamo = findByEventId(page, "563678");
        assertNotNull(dinamo);
        assertEquals("fk-dinamo-moscow-fc-dynamo-makhachkala", dinamo.getSlug());
        assertEquals(Instant.parse("2026-08-09T11:30:00+00:00"), dinamo.getUtcKickoff());
        assertEquals("Динамо М", dinamo.getHomeName());
        assertEquals(1, countByEventId(page, "563678"));

        RuscoreParsedDayPage.CompetitionBlock epl = page.getCompetitions().stream()
                .filter(c -> c.getSeasonId() != null && c.getSeasonId() == 5379)
                .findFirst()
                .orElse(null);
        assertNotNull(epl);
        assertTrue(epl.getTitle().contains("Премьер-Лига"));
    }

    @Test
    void competitionMatchesFilter_byTitleAndSeasonId() throws IOException {
        String html = readFixture("ruscore/day-mini.html");
        RuscoreParsedDayPage page = parser.parse(html, LocalDate.of(2026, 8, 9));
        RuscoreParsedDayPage.CompetitionBlock secondBl = findCompetition(page, "Вторая Бундеслига 26/27");
        assertNotNull(secondBl);
        assertTrue(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "Вторая Бундеслига 26/27"));
        assertTrue(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "Вторая Бундеслига"));
        assertTrue(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "5457"));
        assertTrue(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "germany-2-bundesliga"));
        assertFalse(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "5379"));
        assertFalse(RuscoreDayPageParser.competitionMatchesFilter(secondBl, "Англия"));
    }

    @Test
    void skipsDuplicateEventListedAsMotdAndLeagueRow() {
        String html = """
                <a href="/game/crystal-palace-manchester-city/558546">
                  <h3 data-test-id="player-title-home">Кристал Пэлас</h3>
                  <h3 data-test-id="player-title-away">Манчестер Сити</h3>
                </a>
                <div data-test-id="event-list-header">
                  <a href="/tournament/england-premier-league/5379">
                    <span data-test-id="event-list-header-title-text">Премьер-Лига 26/27</span>
                  </a>
                </div>
                <a href="/game/crystal-palace-manchester-city/558546/summary">
                  <h3 data-test-id="player-title-home">Кристал Пэлас</h3>
                  <h3 data-test-id="player-title-away">Манчестер Сити</h3>
                </a>
                <script id="__NUXT_DATA__" type="application/json">
                558546,"2026-08-28T19:00:00+00:00"
                </script>
                """;
        RuscoreParsedDayPage page = parser.parse(html, LocalDate.of(2026, 8, 28));
        assertEquals(1, countByEventId(page, "558546"));
        RuscoreParsedDayPage.Match match = findByEventId(page, "558546");
        assertNotNull(match);
        assertEquals("Кристал Пэлас", match.getHomeName());
        assertEquals(Instant.parse("2026-08-28T19:00:00Z"), match.getUtcKickoff());
    }

    private static RuscoreParsedDayPage.CompetitionBlock findCompetition(RuscoreParsedDayPage page, String title) {
        for (RuscoreParsedDayPage.CompetitionBlock block : page.getCompetitions()) {
            if (title.equals(block.getTitle())) {
                return block;
            }
        }
        return null;
    }

    private static RuscoreParsedDayPage.Match findByEventId(RuscoreParsedDayPage page, String eventId) {
        for (RuscoreParsedDayPage.CompetitionBlock block : page.getCompetitions()) {
            if (block.getMatches() == null) {
                continue;
            }
            for (RuscoreParsedDayPage.Match match : block.getMatches()) {
                if (eventId.equals(match.getEventId())) {
                    return match;
                }
            }
        }
        return null;
    }

    private static int countByEventId(RuscoreParsedDayPage page, String eventId) {
        int count = 0;
        for (RuscoreParsedDayPage.CompetitionBlock block : page.getCompetitions()) {
            if (block.getMatches() == null) {
                continue;
            }
            for (RuscoreParsedDayPage.Match match : block.getMatches()) {
                if (eventId.equals(match.getEventId())) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String readFixture(String path) throws IOException {
        try (var in = RuscoreDayPageParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
