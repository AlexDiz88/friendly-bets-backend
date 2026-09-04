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

    /** Former post-kickoff window for SCHEDULED-only (4h); candidates now use {@link #LIVE_IN_PLAY_MAX_POLL_SECONDS}. */
    public static final long LIVE_WINDOW_SECONDS = 4 * 3600L;
    /** Stop polling non-terminal matches this long after kickoff (stuck sync); errors logged separately. */
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
     * HTTP candidate: not finalized/FULL; kickoff already passed; still within
     * {@link #LIVE_IN_PLAY_MAX_POLL_SECONDS} after kickoff.
     * Applies to {@code SCHEDULED} stuck after kickoff as well as {@code IN_PLAY} —
     * a missed wake must still be able to catch FT within this window.
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
        // Not started yet — wake scheduler fires at utc_kickoff; do not poll early.
        if (kickoff.isAfter(now)) {
            return false;
        }
        return kickoff.isAfter(now.minusSeconds(LIVE_IN_PLAY_MAX_POLL_SECONDS));
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
