package net.friendly_bets.providers;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Instant;

/**
 * Provider-agnostic apply of {@link FullMatchDetails} onto {@code match_schedules}.
 */
public final class FullMatchPersistSupport {

    private FullMatchPersistSupport() {
    }

    /**
     * Writes score/goals/stats/added-time and marks the schedule finalized for settle.
     *
     * @param match      schedule document to mutate (not saved here)
     * @param details    parsed FULL payload
     * @param providerId {@link ExternalProviderIds} of the FULL provider
     * @param now        finalize / fetched timestamps
     */
    public static void apply(MatchSchedule match, FullMatchDetails details, String providerId, Instant now) {
        if (match == null || details == null) {
            return;
        }
        Instant at = now != null ? now : Instant.now();
        match.setGameScore(details.getGameScore());
        match.setGoals(details.getGoals() != null ? details.getGoals() : java.util.List.of());
        match.setStats(details.getStats());
        match.setAddedTimeFirstHalf(details.getAddedTimeFirstHalf());
        match.setAddedTimeSecondHalf(details.getAddedTimeSecondHalf());
        match.setStatus("FINISHED");
        match.setLiveMinute(null);
        match.setLiveMinuteLabel(null);
        match.setFullDetailsFetchedAt(at);
        match.setFinalizedAt(at);
        match.setFinalizedByProvider(providerId);
        match.setFullMatchNextAttemptAt(null);
        match.setFullMatchNotReadyCount(null);
        match.setFetchedAt(at);
    }
}
