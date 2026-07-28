package net.friendly_bets.football24;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Football24ScheduleParserTest {

    private final Football24ScheduleParser parser = new Football24ScheduleParser(new ObjectMapper());

    @Test
    void parsesNumericToursWithUtcStartingAt_skipsQualifying() {
        String json = """
                {"data":[
                  {"round":"2-й кваліфікаційний раунд","fixtures":[
                    {"teamHome":{"name":"КуПС"},"teamAway":{"name":"Сабах"},
                     "startingAt":"2026-07-28T15:00:00.000Z","score":null,"teamHomeScore":null,"teamAwayScore":null}
                  ]},
                  {"round":"ТУР 1","fixtures":[
                    {"teamHome":{"name":"Арсенал"},"teamAway":{"name":"Ковентрі Сіті"},
                     "startingAt":"2026-08-21T19:00:00.000Z","score":null,"teamHomeScore":null,"teamAwayScore":null},
                    {"teamHome":{"name":"Халл Сіті"},"teamAway":{"name":"Манчестер Юнайтед"},
                     "startingAt":"2026-08-22T11:30:00.000Z","score":null,"teamHomeScore":null,"teamAwayScore":null}
                  ]},
                  {"round":"ТУР 2","fixtures":[
                    {"teamHome":{"name":"Челсі"},"teamAway":{"name":"Ліверпуль"},
                     "startingAt":"2026-08-28T19:00:00.000Z","score":"1:0","teamHomeScore":1,"teamAwayScore":0}
                  ]}
                ]}
                """;

        Football24ParsedSchedule parsed = parser.parseFixturesRounds(json, 1286);
        assertEquals(1286, parsed.getSeasonId());
        assertEquals(2, parsed.getRounds().size());
        assertEquals(1, parsed.getRounds().get(0).getNumber());
        assertEquals(2, parsed.getRounds().get(0).getMatches().size());

        Football24ParsedSchedule.Match first = parsed.getRounds().get(0).getMatches().get(0);
        assertEquals("Арсенал", first.getHomeName());
        assertEquals("Ковентрі Сіті", first.getAwayName());
        assertEquals(Instant.parse("2026-08-21T19:00:00Z"), first.getUtcKickoff());
        assertEquals("SCHEDULED", first.getStatus());

        Football24ParsedSchedule.Match finished = parsed.getRounds().get(1).getMatches().get(0);
        assertEquals("FINISHED", finished.getStatus());
        assertEquals(Instant.parse("2026-08-28T19:00:00Z"), finished.getUtcKickoff());
    }

    @Test
    void resolveSeasonId_prefersYearPrefixOverOtherCurrent() {
        String json = """
                {"data":[
                  {"id":1286,"name":"2026/2027","isCurrent":true},
                  {"id":1038,"name":"2025/2026","isCurrent":true},
                  {"id":33,"name":"2024/2025","isCurrent":false}
                ]}
                """;
        assertEquals(OptionalInt.of(1286), parser.resolveSeasonId(json, 2026));
        assertEquals(OptionalInt.of(1038), parser.resolveSeasonId(json, 2025));
    }

    @Test
    void wallClockWithoutZ_isNotAcceptedAsKickoff() {
        assertNull(parser.parseStartingAt("21.08.2026 22:00"));
        assertNull(parser.parseStartingAt(""));
    }

    @Test
    void qualifyingDetection() {
        assertTrue(Football24ScheduleParser.isQualifyingRound("2-й кваліфікаційний раунд"));
        assertTrue(Football24ScheduleParser.isQualifyingRound("3-й квалификационный раунд"));
        assertFalse(Football24ScheduleParser.isQualifyingRound("ТУР 1"));
        assertEquals(OptionalInt.of(1), Football24ScheduleParser.parseTourNumber("ТУР 1"));
        assertEquals(OptionalInt.of(12), Football24ScheduleParser.parseTourNumber("тур 12"));
    }

    @Test
    void parseTeamNamesFromMatchdayOne() {
        String json = """
                {"data":[
                  {"round":"ТУР 1","fixtures":[
                    {"teamHome":{"name":"Арсенал"},"teamAway":{"name":"Ковентрі Сіті"},
                     "startingAt":"2026-08-21T19:00:00.000Z"}
                  ]}
                ]}
                """;
        List<String> names = parser.parseTeamNamesFromMatchday(json, 1286, 1);
        assertEquals(2, names.size());
        assertTrue(names.contains("Арсенал"));
        assertTrue(names.contains("Ковентрі Сіті"));
    }
}
