package net.friendly_bets.wc26;

import net.friendly_bets.dto.Wc26ScheduleMatchDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Wc26StandingsLiveScoresTest {

    @Test
    void byTeam_mapsTeamPerspectiveScores() {
        Wc26SchedulePageDto schedule = Wc26SchedulePageDto.builder()
                .matches(List.of(
                        Wc26ScheduleMatchDto.builder()
                                .home("SCO")
                                .away("BRA")
                                .status("IN_PLAY")
                                .finalized(false)
                                .liveMinuteLabel("25'")
                                .scoreView("0:1")
                                .build(),
                        Wc26ScheduleMatchDto.builder()
                                .home("MAR")
                                .away("HAI")
                                .status("IN_PLAY")
                                .finalized(false)
                                .liveMinuteLabel("26'")
                                .scoreView("0:1")
                                .build()
                ))
                .build();

        Map<String, String> byTeam = Wc26StandingsLiveScores.byTeam(schedule);

        assertEquals("0:1", byTeam.get("SCO"));
        assertEquals("1:0", byTeam.get("BRA"));
        assertEquals("0:1", byTeam.get("MAR"));
        assertEquals("1:0", byTeam.get("HAI"));
    }

    @Test
    void compactScoreLine_stripsHalftimeSuffix() {
        assertEquals("2:1", Wc26StandingsLiveScores.compactScoreLine("2:1 (0:0)"));
        assertNull(Wc26StandingsLiveScores.compactScoreLine("—"));
    }

    @Test
    void scoreOrDefault_usesZeroWhenMissing() {
        assertEquals("0:0", Wc26StandingsLiveScores.scoreOrDefault("—"));
        assertEquals("0:1", Wc26StandingsLiveScores.scoreOrDefault("0:1"));
    }

    @Test
    void merge_fifaCalendarOverridesSchedule() {
        Map<String, String> merged = Wc26StandingsLiveScores.merge(
                Map.of("MAR", "0:1"),
                Map.of("MAR", "1:1"));
        assertEquals("1:1", merged.get("MAR"));
    }

    @Test
    void byTeam_includesLiveMatchEvenWithoutScoreLine() {
        Wc26SchedulePageDto schedule = Wc26SchedulePageDto.builder()
                .matches(List.of(
                        Wc26ScheduleMatchDto.builder()
                                .home("SCO")
                                .away("BRA")
                                .status("IN_PLAY")
                                .finalized(false)
                                .liveMinuteLabel("25'")
                                .scoreView("—")
                                .build()
                ))
                .build();

        assertEquals("0:0", Wc26StandingsLiveScores.byTeam(schedule).get("SCO"));
        assertEquals("0:0", Wc26StandingsLiveScores.byTeam(schedule).get("BRA"));
    }

    @Test
    void byTeam_ignoresFinishedMatches() {
        Wc26SchedulePageDto schedule = Wc26SchedulePageDto.builder()
                .matches(List.of(
                        Wc26ScheduleMatchDto.builder()
                                .home("SCO")
                                .away("BRA")
                                .status("FINISHED")
                                .finalized(true)
                                .scoreView("0:2")
                                .build()
                ))
                .build();

        assertTrue(Wc26StandingsLiveScores.byTeam(schedule).isEmpty());
    }
}
