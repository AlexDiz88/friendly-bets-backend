package net.friendly_bets.oddsapi;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.models.odds.Odds;

import java.util.Optional;

/**
 * Marathonbet — primary source for production merged odds.
 */
final class MarathonbetMergedOddsGuard {

    private MarathonbetMergedOddsGuard() {
    }

    static boolean hasProductionMarathonOdds(Optional<Odds> merged) {
        if (merged.isEmpty()) {
            return false;
        }
        Odds doc = merged.get();
        if (doc.getMarketGroups() == null || doc.getMarketGroups().isEmpty()) {
            return false;
        }
        return doc.getBookmakers() != null
                && doc.getBookmakers().stream()
                .anyMatch(b -> MarathonbetBookmaker.KEY.equalsIgnoreCase(b));
    }
}
