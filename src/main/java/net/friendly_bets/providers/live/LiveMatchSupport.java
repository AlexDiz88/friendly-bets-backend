package net.friendly_bets.providers.live;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Shared LIVE-layer polling windows and status helpers.
 * Used by every {@link net.friendly_bets.providers.LiveMatchProvider} and the layer wake scheduler —
 * providers must not redefine candidate/finished rules.
 */
public final class LiveMatchSupport {

    public static final long LIVE_WINDOW_SECONDS = 4 * 3600L;
    /** Stop polling IN_PLAY matches long after kickoff (stuck sync); errors logged separately. */
    public static final long LIVE_IN_PLAY_MAX_POLL_SECONDS = 6 * 3600L;

    private static final Set<String> FINISHED = Set.of(
            "FINISHED", "AWARDED", "COMPLETED", "FT", "AET", "PEN"
    );
    private static final Set<String> CANCELED = Set.of("CANCELED", "CANCELLED");
    private static final Set<String> IN_PLAY = Set.of(
            "LIVE", "IN_PLAY", "PAUSED", "HALFTIME", "EXTRA_TIME", "PENALTY_SHOOTOUT"
    );

    private LiveMatchSupport() {
    }

    /**
     * HTTP candidate: not finalized/FULL; if still in play → true;
     * else kickoff in {@code [now - LIVE_WINDOW, now]}.
     */
    public static boolean isLiveHttpCandidate(MatchSchedule schedule, Instant now) {
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
        if (FINISHED.contains(status) || CANCELED.contains(status)) {
            return false;
        }
        if (IN_PLAY.contains(status)) {
            return kickoff.isAfter(now.minusSeconds(LIVE_IN_PLAY_MAX_POLL_SECONDS));
        }
        return !kickoff.isAfter(now) && kickoff.isAfter(now.minusSeconds(LIVE_WINDOW_SECONDS));
    }

    /** Not finished/canceled/finalized and missing kickoff — LIVE cannot resolve. */
    public static boolean isMissingUtcKickoffSkip(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getUtcKickoff() != null) {
            return false;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return false;
        }
        String status = normalize(schedule.getStatus());
        return !FINISHED.contains(status) && !CANCELED.contains(status);
    }

    public static boolean needsFullMatch(MatchSchedule schedule) {
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
    public static Instant upcomingKickoffOrNull(MatchSchedule schedule, Instant now) {
        if (schedule == null || now == null) {
            return null;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return null;
        }
        String status = normalize(schedule.getStatus());
        if (FINISHED.contains(status) || CANCELED.contains(status) || IN_PLAY.contains(status)) {
            return null;
        }
        Instant kickoff = schedule.getUtcKickoff();
        if (kickoff == null || !kickoff.isAfter(now)) {
            return null;
        }
        return kickoff;
    }

    public static boolean isFinishedStatus(String status) {
        return FINISHED.contains(normalize(status));
    }

    public static boolean isCanceledStatus(String status) {
        return CANCELED.contains(normalize(status));
    }

    public static boolean isTerminalNoPoll(String status) {
        String n = normalize(status);
        return FINISHED.contains(n) || CANCELED.contains(n);
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
