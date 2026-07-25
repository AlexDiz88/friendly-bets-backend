package net.friendly_bets.marathonbet;

/**
 * When to issue Marathonbet SSE for a pending match.
 */
public enum OddsSsePolicy {
    /** Cron: missing odds always; existing odds only near kickoff ({@code sseRefreshWithinHours}). */
    REFRESH_WINDOW,
    /** Admin default: only matches that have no odds yet. */
    MISSING_ONLY,
    /** Admin force: fetch selected matches even if odds already exist. */
    FORCE
}
