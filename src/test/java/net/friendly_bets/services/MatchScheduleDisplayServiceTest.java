package net.friendly_bets.services;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScheduleDisplayServiceTest {

    @Test
    void liveScoreWithoutFinalizedAt_isNotFinalized() {
        MatchSchedule live = MatchSchedule.builder()
                .status("LIVE")
                .gameScore(GameScore.builder().fullTime("0:0").build())
                .liveMinuteLabel("15")
                .build();

        assertFalse(MatchScheduleDisplayService.isFinalized(live));
    }

    @Test
    void liveFinishedWithoutFinalizedAt_isNotFinalized() {
        MatchSchedule finished = MatchSchedule.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("2:1").build())
                .build();

        assertFalse(MatchScheduleDisplayService.isFinalized(finished));
    }

    @Test
    void fullMatchFinalizedAt_isFinalized() {
        MatchSchedule locked = MatchSchedule.builder()
                .status("FINISHED")
                .gameScore(GameScore.builder().fullTime("1:0").build())
                .finalizedAt(Instant.parse("2026-08-21T20:00:00Z"))
                .build();

        assertTrue(MatchScheduleDisplayService.isFinalized(locked));
    }
}
