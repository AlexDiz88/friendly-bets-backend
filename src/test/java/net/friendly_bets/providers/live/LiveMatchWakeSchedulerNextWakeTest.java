package net.friendly_bets.providers.live;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveMatchWakeSchedulerNextWakeTest {

    private static final Instant NOW = Instant.parse("2026-08-24T21:02:26Z");
    private static final long TICK_MS = 300_000L;

    @Test
    void pastDue_waitsFullTick_doesNotFireImmediately() {
        Instant pastDue = NOW.minusSeconds(60);
        Instant next = LiveMatchWakeScheduler.nextWakeAfterPoll(NOW, pastDue, TICK_MS);
        assertEquals(NOW.plusMillis(TICK_MS), next);
    }

    @Test
    void futureDueWithinTick_wakesAtDue() {
        Instant due = NOW.plusSeconds(120);
        Instant next = LiveMatchWakeScheduler.nextWakeAfterPoll(NOW, due, TICK_MS);
        assertEquals(due, next);
    }

    @Test
    void futureDueAfterTick_wakesAtTick() {
        Instant due = NOW.plusSeconds(900);
        Instant next = LiveMatchWakeScheduler.nextWakeAfterPoll(NOW, due, TICK_MS);
        assertEquals(NOW.plusMillis(TICK_MS), next);
    }

    @Test
    void nullDue_waitsFullTick() {
        Instant next = LiveMatchWakeScheduler.nextWakeAfterPoll(NOW, null, TICK_MS);
        assertEquals(NOW.plusMillis(TICK_MS), next);
    }

    @Test
    void heartbeat_capsDistantKickoff() {
        Instant kickoff = NOW.plusSeconds(8 * 3600L);
        Instant next = LiveMatchWakeScheduler.capWakeByHeartbeat(
                NOW, kickoff, LiveMatchWakeScheduler.WAKE_HEARTBEAT_MS);
        assertEquals(NOW.plusMillis(LiveMatchWakeScheduler.WAKE_HEARTBEAT_MS), next);
    }

    @Test
    void heartbeat_keepsNearKickoff() {
        Instant kickoff = NOW.plusSeconds(120);
        Instant next = LiveMatchWakeScheduler.capWakeByHeartbeat(
                NOW, kickoff, LiveMatchWakeScheduler.WAKE_HEARTBEAT_MS);
        assertEquals(kickoff, next);
    }
}
