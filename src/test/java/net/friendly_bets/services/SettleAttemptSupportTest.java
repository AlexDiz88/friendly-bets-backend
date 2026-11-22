package net.friendly_bets.services;

import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettleAttemptSupportTest {

    private static final Instant T0 = Instant.parse("2026-11-22T12:00:00Z");

    @Test
    void applyFailure_blocksAfterMaxFailures() {
        MatchResultSyncProperties props = new MatchResultSyncProperties();
        props.setSettleMaxFailures(3);
        props.setSettleBlockAfterMaxMs(3_600_000L);
        MatchSchedule schedule = MatchSchedule.builder().id("ms-1").build();

        SettleAttemptSupport.applyFailure(schedule, T0, props);
        assertEquals(1, schedule.getSettleFailCount());
        assertNull(schedule.getSettleNextAttemptAt());
        assertTrue(SettleAttemptSupport.isAttemptDue(schedule, T0));

        SettleAttemptSupport.applyFailure(schedule, T0, props);
        assertEquals(2, schedule.getSettleFailCount());
        assertNull(schedule.getSettleNextAttemptAt());

        SettleAttemptSupport.applyFailure(schedule, T0, props);
        assertEquals(0, schedule.getSettleFailCount());
        assertEquals(T0.plusSeconds(3600), schedule.getSettleNextAttemptAt());
        assertFalse(SettleAttemptSupport.isAttemptDue(schedule, T0.plusSeconds(3599)));
        assertTrue(SettleAttemptSupport.isAttemptDue(schedule, T0.plusSeconds(3600)));
    }

    @Test
    void clearAttemptState_resetsBudget() {
        MatchSchedule schedule = MatchSchedule.builder()
                .settleFailCount(2)
                .settleNextAttemptAt(T0.plusSeconds(3600))
                .build();
        SettleAttemptSupport.clearAttemptState(schedule);
        assertNull(schedule.getSettleFailCount());
        assertNull(schedule.getSettleNextAttemptAt());
        assertTrue(SettleAttemptSupport.isAttemptDue(schedule, T0));
    }
}
