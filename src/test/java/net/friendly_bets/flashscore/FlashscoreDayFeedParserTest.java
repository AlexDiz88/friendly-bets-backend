package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreDayFeedParserTest {

    private final FlashscoreDayFeedParser parser = new FlashscoreDayFeedParser();

    @Test
    void parsesMatchesKickoffAndCompetitionHeaders() throws IOException {
        String feed = readFixture("flashscore/day-nurnberg-mini.feed");
        FlashscoreParsedDayPage page = parser.parse(feed, LocalDate.of(2026, 8, 9));
        assertNotNull(page.getCompetitions());
        assertEquals(1, page.getCompetitions().size());

        FlashscoreParsedDayPage.CompetitionBlock bl = page.getCompetitions().get(0);
        assertTrue(bl.getTitle().contains("2. Bundesliga"));
        assertEquals(1, bl.getMatches().size());

        FlashscoreParsedDayPage.Match match = bl.getMatches().get(0);
        assertEquals("nNUWaFf5", match.getEventId());
        assertEquals(Instant.parse("2026-08-09T11:30:00+00:00"), match.getUtcKickoff());
        assertEquals("Nurnberg", match.getHomeName());
        assertEquals("SG Dynamo Dresden", match.getAwayName());
        assertEquals("3:0", match.getScoreText());
        assertEquals("finished", match.getStatusText());
    }

    @Test
    void competitionMatchesFilter_byTitleAndStageId() throws IOException {
        String feed = readFixture("flashscore/day-nurnberg-mini.feed");
        FlashscoreParsedDayPage page = parser.parse(feed, LocalDate.of(2026, 8, 9));
        FlashscoreParsedDayPage.CompetitionBlock block = page.getCompetitions().get(0);
        assertTrue(FlashscoreDayFeedParser.competitionMatchesFilter(block, "2. Bundesliga"));
        assertTrue(FlashscoreDayFeedParser.competitionMatchesFilter(block, "6khmdCet"));
        assertTrue(FlashscoreDayFeedParser.competitionMatchesFilter(block, "germany/2-bundesliga"));
        assertFalse(FlashscoreDayFeedParser.competitionMatchesFilter(block, "Premier League"));
    }

    private static String readFixture(String path) throws IOException {
        try (var in = FlashscoreDayFeedParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
