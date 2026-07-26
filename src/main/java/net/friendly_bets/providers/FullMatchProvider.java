package net.friendly_bets.providers;

import net.friendly_bets.models.schedule.MatchSchedule;

/**
 * Layer FULL_MATCH: one-shot fetch of complete match details after the match is finished.
 */
public interface FullMatchProvider extends ExternalDataProvider {

    /**
     * Fetch and persist full details for a finished match schedule.
     *
     * @return updated schedule document
     */
    MatchSchedule fetchAndPersistFullDetails(MatchSchedule match);
}
