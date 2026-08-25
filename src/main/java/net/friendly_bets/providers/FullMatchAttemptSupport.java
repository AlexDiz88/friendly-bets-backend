package net.friendly_bets.providers;

import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.live.LiveMatchSupport;

import java.time.Duration;
import java.time.Instant;

/**
 * FULL_MATCH attempt timing: initial delay after LIVE→FINISHED, then 5‑min / hourly deferrals
 * after any unsuccessful attempt (not-ready, not-found, parse/HTTP failure).
 */
public final class FullMatchAttemptSupport {

    private FullMatchAttemptSupport() {
    }

    public static boolean isAttemptDue(MatchSchedule schedule, Instant now, MatchResultSyncProperties properties) {
        if (schedule == null || now == null || !LiveMatchSupport.needsFullMatch(schedule)) {
            return false;
        }
        Instant due = resolveDueAt(schedule, properties);
        return !now.isBefore(due);
    }

    public static Instant resolveDueAt(MatchSchedule schedule, MatchResultSyncProperties properties) {
        if (schedule == null) {
            return Instant.EPOCH;
        }
        if (schedule.getFullMatchNextAttemptAt() != null) {
            return schedule.getFullMatchNextAttemptAt();
        }
        Instant detected = schedule.getLiveFinishedDetectedAt();
        if (detected != null) {
            return detected.plus(initialDelay(properties));
        }
        // Legacy FINISHED rows without stamp: treat as due immediately once discovered.
        return Instant.EPOCH;
    }

    /**
     * After this failure is recorded, count becomes count+1: 1st unsuccessful attempt → 5 min,
     * 2nd and later → hourly. Applies to not-ready and hard failures (not-found, parse, HTTP).
     */
    public static Instant nextAttemptAfterFailure(
            MatchSchedule schedule,
            Instant now,
            MatchResultSyncProperties properties
    ) {
        int count = schedule.getFullMatchNotReadyCount() != null ? schedule.getFullMatchNotReadyCount() : 0;
        if (count + 1 >= 2) {
            return now.plus(hourlyDelay(properties));
        }
        return now.plus(retryDelay(properties));
    }

    /** @see #nextAttemptAfterFailure */
    public static Instant nextAttemptAfterNotReady(
            MatchSchedule schedule,
            Instant now,
            MatchResultSyncProperties properties
    ) {
        return nextAttemptAfterFailure(schedule, now, properties);
    }

    public static boolean shouldLogNotReady(MatchSchedule schedule) {
        int count = schedule.getFullMatchNotReadyCount() != null ? schedule.getFullMatchNotReadyCount() : 0;
        // Log starting from the second consecutive not-ready response.
        return count + 1 >= 2;
    }

    public static void clearAttemptState(MatchSchedule schedule) {
        if (schedule == null) {
            return;
        }
        schedule.setFullMatchNextAttemptAt(null);
        schedule.setFullMatchNotReadyCount(null);
    }

    private static Duration initialDelay(MatchResultSyncProperties properties) {
        return Duration.ofMillis(Math.max(0L, properties.getFullMatchInitialDelayMs()));
    }

    private static Duration retryDelay(MatchResultSyncProperties properties) {
        return Duration.ofMillis(Math.max(0L, properties.getFullMatchRetryDelayMs()));
    }

    private static Duration hourlyDelay(MatchResultSyncProperties properties) {
        return Duration.ofMillis(Math.max(0L, properties.getFullMatchHourlyDelayMs()));
    }
}
