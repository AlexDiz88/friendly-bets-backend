package net.friendly_bets.matchschedule;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleStatusWriteSupportTest {

    @Test
    void appliesStatusOnNewOrScheduledDocument() {
        MatchSchedule blank = MatchSchedule.builder().build();
        ScheduleStatusWriteSupport.applyFromSchedule(blank, "SCHEDULED");
        assertEquals("SCHEDULED", blank.getStatus());

        MatchSchedule scheduled = MatchSchedule.builder().status("SCHEDULED").build();
        ScheduleStatusWriteSupport.applyFromSchedule(scheduled, "FINISHED");
        assertEquals("FINISHED", scheduled.getStatus());
    }

    @Test
    void doesNotOverwriteTerminalOrLiveOrFinalized() {
        MatchSchedule finished = MatchSchedule.builder().status("FINISHED").build();
        ScheduleStatusWriteSupport.applyFromSchedule(finished, "SCHEDULED");
        assertEquals("FINISHED", finished.getStatus());

        MatchSchedule live = MatchSchedule.builder().status("IN_PLAY").build();
        ScheduleStatusWriteSupport.applyFromSchedule(live, "SCHEDULED");
        assertEquals("IN_PLAY", live.getStatus());

        MatchSchedule finalized = MatchSchedule.builder()
                .status("SCHEDULED")
                .finalizedAt(Instant.parse("2026-10-11T15:10:20.412Z"))
                .build();
        ScheduleStatusWriteSupport.applyFromSchedule(finalized, "SCHEDULED");
        assertEquals("SCHEDULED", finalized.getStatus());
    }

    @Test
    void ignoresNullIncoming() {
        MatchSchedule existing = MatchSchedule.builder().status("SCHEDULED").build();
        ScheduleStatusWriteSupport.applyFromSchedule(existing, null);
        assertEquals("SCHEDULED", existing.getStatus());
    }
}
