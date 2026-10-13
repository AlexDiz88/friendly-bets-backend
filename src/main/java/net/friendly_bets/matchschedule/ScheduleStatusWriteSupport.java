package net.friendly_bets.matchschedule;

import net.friendly_bets.models.schedule.MatchSchedule;

/**
 * SCHEDULE may set fixture status, but must not overwrite LIVE/FULL progression
 * (live statuses, terminal statuses, or already finalized documents).
 */
public final class ScheduleStatusWriteSupport {

    private ScheduleStatusWriteSupport() {
    }

    public static void applyFromSchedule(MatchSchedule existing, String incomingStatus) {
        if (existing == null || incomingStatus == null) {
            return;
        }
        if (existing.getFinalizedAt() != null) {
            return;
        }
        String current = MatchStatuses.normalize(existing.getStatus());
        if (current != null
                && (MatchStatuses.isTerminal(current) || MatchStatuses.LIVE.contains(current))) {
            return;
        }
        existing.setStatus(incomingStatus);
    }
}
