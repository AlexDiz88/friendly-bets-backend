package net.friendly_bets.providers;

import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullMatchAttemptSupportTest {

    private static final Instant T0 = Instant.parse("2026-08-03T12:00:00Z");

    private final MatchResultSyncProperties properties = defaults();

    @Test
    void initialDelay_fromLiveFinishedDetectedAt() {
        MatchSchedule schedule = MatchSchedule.builder()
                .status("FINISHED")
                .liveFinishedDetectedAt(T0)
                .build();
        Instant due = FullMatchAttemptSupport.resolveDueAt(schedule, properties);
        assertEquals(T0.plusSeconds(300), due);
        assertFalse(FullMatchAttemptSupport.isAttemptDue(schedule, T0.plusSeconds(299), properties));
        assertTrue(FullMatchAttemptSupport.isAttemptDue(schedule, T0.plusSeconds(300), properties));
    }

    @Test
    void explicitNextAttemptOverridesInitialDelay() {
        MatchSchedule schedule = MatchSchedule.builder()
                .status("FINISHED")
                .liveFinishedDetectedAt(T0)
                .fullMatchNextAttemptAt(T0.plusSeconds(3600))
                .build();
        assertEquals(T0.plusSeconds(3600), FullMatchAttemptSupport.resolveDueAt(schedule, properties));
    }

    @Test
    void notReady_firstRetryFiveMinutes_secondHourlyAndLogs() {
        MatchSchedule first = MatchSchedule.builder().status("FINISHED").fullMatchNotReadyCount(null).build();
        assertFalse(FullMatchAttemptSupport.shouldLogNotReady(first));
        assertEquals(T0.plusSeconds(300), FullMatchAttemptSupport.nextAttemptAfterNotReady(first, T0, properties));

        MatchSchedule second = MatchSchedule.builder().status("FINISHED").fullMatchNotReadyCount(1).build();
        assertTrue(FullMatchAttemptSupport.shouldLogNotReady(second));
        assertEquals(T0.plusSeconds(3600), FullMatchAttemptSupport.nextAttemptAfterNotReady(second, T0, properties));
    }

    @Test
    void legacyFinishedWithoutStamp_dueImmediately() {
        MatchSchedule schedule = MatchSchedule.builder().status("FINISHED").build();
        assertTrue(FullMatchAttemptSupport.isAttemptDue(schedule, T0, properties));
    }

    private static MatchResultSyncProperties defaults() {
        MatchResultSyncProperties p = new MatchResultSyncProperties();
        p.setFullMatchInitialDelayMs(300_000L);
        p.setFullMatchRetryDelayMs(300_000L);
        p.setFullMatchHourlyDelayMs(3_600_000L);
        return p;
    }
}
