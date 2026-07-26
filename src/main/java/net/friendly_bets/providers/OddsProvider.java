package net.friendly_bets.providers;

import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

import java.util.List;

/**
 * Layer ODDS: scrape bookmaker odds and persist via {@code MappedOddsQuote} → {@code odds}.
 */
public interface OddsProvider extends ExternalDataProvider {

    /**
     * Sync odds for a league slot (current matchday window as implemented by the provider).
     *
     * @param matchScheduleIds optional filter; empty = all matches in the provider slot window
     */
    OddsSyncResult syncLeagueSlot(Season season, League league, String matchdaySlotId, List<String> matchScheduleIds);

    record OddsSyncResult(
            String leagueCode,
            int matchday,
            int matched,
            int persisted,
            int skipped,
            String message
    ) {
    }
}
