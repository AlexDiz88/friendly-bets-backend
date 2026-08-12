package net.friendly_bets.providers;

import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;

/**
 * Layer STANDINGS: fetch league table from external source and persist snapshot in {@code standings}.
 */
public interface StandingsProvider extends ExternalDataProvider {

    StandingsSyncResultDto syncByLeagueCode(String leagueCode);

    StandingsSyncResultDto syncLeague(Season season, League league);

    /**
     * Provider-specific fetch + parse. Persistence is handled by {@link StandingsPersistSupport}.
     */
    StandingsTableSnapshot fetchTableSnapshot(Season season, League league);
}
