package net.friendly_bets.providers.odds;

/**
 * When the ODDS layer should fetch odds for a not-started match.
 */
public enum OddsFetchPolicy {
    /** Missing odds always; existing odds only within refresh-within-hours of kickoff. */
    REFRESH_WINDOW,
    /** Only matches that have no odds yet. */
    MISSING_ONLY,
    /** Fetch even if odds already exist (near-kickoff cron refresh). */
    FORCE
}
