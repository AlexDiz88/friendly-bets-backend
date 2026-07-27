package net.friendly_bets.twentyfourscore;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * LIVE polling windows for 24score: HTTP only from kickoff until finished;
 * FINISHED without FULL is handled separately (no LIVE HTTP).
 */
final class TwentyFourScoreLiveSupport {

    static final long LIVE_WINDOW_SECONDS = 4 * 3600L;
    private static final Set<String> FINISHED = Set.of("FINISHED", "AWARDED", "COMPLETED", "FT", "AET", "PEN");
    private static final Set<String> IN_PLAY = Set.of("LIVE", "IN_PLAY", "PAUSED", "HALFTIME");

    private TwentyFourScoreLiveSupport() {
    }

    static boolean isLiveHttpCandidate(MatchSchedule schedule, Instant now) {
        if (schedule == null || now == null) {
            return false;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return false;
        }
        Instant kickoff = schedule.getUtcKickoff();
        if (kickoff == null) {
            return false;
        }
        String status = normalize(schedule.getStatus());
        if (FINISHED.contains(status)) {
            return false;
        }
        if (IN_PLAY.contains(status)) {
            return true;
        }
        return !kickoff.isAfter(now) && kickoff.isAfter(now.minusSeconds(LIVE_WINDOW_SECONDS));
    }

    /** Not finished/finalized and missing kickoff — ODDS/LIVE/FULL cannot resolve. */
    static boolean isMissingUtcKickoffSkip(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getUtcKickoff() != null) {
            return false;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return false;
        }
        return !FINISHED.contains(normalize(schedule.getStatus()));
    }

    static boolean needsFullMatch(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getFullDetailsFetchedAt() != null) {
            return false;
        }
        return FINISHED.contains(normalize(schedule.getStatus()));
    }

    /**
     * Future kickoff we should wake on (not yet started, not already finalized/FULL).
     */
    static Instant upcomingKickoffOrNull(MatchSchedule schedule, Instant now) {
        if (schedule == null || now == null) {
            return null;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return null;
        }
        String status = normalize(schedule.getStatus());
        if (FINISHED.contains(status) || IN_PLAY.contains(status)) {
            return null;
        }
        Instant kickoff = schedule.getUtcKickoff();
        if (kickoff == null || !kickoff.isAfter(now)) {
            return null;
        }
        return kickoff;
    }

    static boolean isFinishedStatus(String status) {
        return FINISHED.contains(normalize(status));
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
