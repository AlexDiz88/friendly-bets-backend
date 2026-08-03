package net.friendly_bets.providers;

import net.friendly_bets.models.schedule.MatchSchedule;

/**
 * Layer FULL_MATCH: one-shot fetch of complete match details after the match is finished.
 * Must refuse finalize (throw {@link net.friendly_bets.exceptions.FullMatchNotReadyException})
 * when the source status is not finished — orchestrator will defer.
 */
public interface FullMatchProvider extends ExternalDataProvider {

    /**
     * Fetch and persist full details for a finished match schedule.
     *
     * @return updated schedule document
     * @throws net.friendly_bets.exceptions.FullMatchNotReadyException if provider status is still in-play
     */
    MatchSchedule fetchAndPersistFullDetails(MatchSchedule match);
}
