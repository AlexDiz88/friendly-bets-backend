package net.friendly_bets.twentyfourscore;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwentyFourScoreScheduleParserTest {

    private final TwentyFourScoreScheduleParser parser = new TwentyFourScoreScheduleParser();

    @Test
    void parseDailyPageFindsWorldCupMatches() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/twentyfourscore/date-2026-06-15.html"));
        var matches = parser.parseDailyPage(
                html,
                LocalDate.of(2026, 6, 15),
                TwentyFourScoreCompetitionMapping.worldCupPathMarker()
        );
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().anyMatch(m -> m.getExternalMatchId() == 849893L));
        TwentyFourScoreListMatch swedenTunisia = matches.stream()
                .filter(m -> m.getExternalMatchId() == 849893L)
                .findFirst()
                .orElseThrow();
        assertEquals("Швеция", swedenTunisia.getHomeTeamName());
        assertEquals("Тунис", swedenTunisia.getAwayTeamName());
        assertEquals("5:1", swedenTunisia.getFullTimeScore());
        assertEquals("2:1", swedenTunisia.getFirstHalfScore());
    }

    @Test
    void parseScoreTextHandlesExtraTimeAndPenalties() {
        TwentyFourScoreScheduleParser.ScoreParts parts = TwentyFourScoreScheduleParser.parseScoreText(
                "2:2 (0:1) дв 0:0, пен 2:3"
        );
        assertEquals("2:2", parts.fullTime());
        assertEquals("0:1", parts.firstHalf());
        assertEquals("0:0", parts.extraTime());
        assertEquals("2:3", parts.penalty());
    }

    @Test
    void parseScoreTextHandlesCompactExtraTimeAndPenaltiesInsideParentheses() {
        TwentyFourScoreScheduleParser.ScoreParts parts = TwentyFourScoreScheduleParser.parseScoreText(
                "1:1 (0:1дв0:0,пен3:4)"
        );
        assertEquals("1:1", parts.fullTime());
        assertEquals("0:1", parts.firstHalf());
        assertEquals("0:0", parts.extraTime());
        assertEquals("3:4", parts.penalty());
    }

    @Test
    void parseScoreTextHandlesHalfTimeFieldWithExtrasOnly() {
        TwentyFourScoreScheduleParser.ScoreParts parts = TwentyFourScoreScheduleParser.parseScoreText(
                "0:1дв0:0,пен3:4"
        );
        assertEquals("0:1", parts.fullTime());
        assertNull(parts.firstHalf());
        assertEquals("0:0", parts.extraTime());
        assertEquals("3:4", parts.penalty());
    }

    @Test
    void parseScoreTextDetectsLiveMinuteOnDailyPage() {
        TwentyFourScoreScheduleParser.ScoreParts parts = TwentyFourScoreScheduleParser.parseScoreText(
                "0:1 (0:1) 50'"
        );
        assertEquals("0:1", parts.fullTime());
        assertEquals("0:1", parts.firstHalf());
        assertEquals("50'", parts.liveMinuteLabel());
        assertEquals("Идёт 50'", parts.statusText());
    }

    @Test
    void parseScoreTextMarksFinishedWhenNoLiveMinute() {
        TwentyFourScoreScheduleParser.ScoreParts parts = TwentyFourScoreScheduleParser.parseScoreText(
                "5:1 (2:1)"
        );
        assertEquals("5:1", parts.fullTime());
        assertEquals("2:1", parts.firstHalf());
        assertEquals("Завершен", parts.statusText());
    }
}
