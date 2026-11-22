package net.friendly_bets.services;

import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Duration;
import java.time.Instant;

/**
 * Auto-settle retry budget: up to N consecutive failures, then block for 1 hour.
 * Manual admin settle is not gated by this state.
 */
public final class SettleAttemptSupport {

    private SettleAttemptSupport() {
    }

    public static boolean isAttemptDue(MatchSchedule schedule, Instant now) {
        if (schedule == null || now == null) {
            return false;
        }
        Instant nextAt = schedule.getSettleNextAttemptAt();
        return nextAt == null || !now.isBefore(nextAt);
    }

    /**
     * Records a failed settle attempt. After {@code settleMaxFailures} consecutive failures
     * sets a block until {@code now + settleBlockAfterMaxMs} and resets the counter for the
     * next window after the cooldown.
     */
    public static void applyFailure(MatchSchedule schedule, Instant now, MatchResultSyncProperties properties) {
        if (schedule == null || now == null) {
            return;
        }
        int max = Math.max(1, properties != null ? properties.getSettleMaxFailures() : 3);
        long blockMs = properties != null ? Math.max(0L, properties.getSettleBlockAfterMaxMs()) : 3_600_000L;
        int count = (schedule.getSettleFailCount() != null ? schedule.getSettleFailCount() : 0) + 1;
        if (count >= max) {
            schedule.setSettleFailCount(0);
            schedule.setSettleNextAttemptAt(now.plus(Duration.ofMillis(blockMs)));
        } else {
            schedule.setSettleFailCount(count);
            schedule.setSettleNextAttemptAt(null);
        }
    }

    public static void clearAttemptState(MatchSchedule schedule) {
        if (schedule == null) {
            return;
        }
        schedule.setSettleFailCount(null);
        schedule.setSettleNextAttemptAt(null);
    }
}
