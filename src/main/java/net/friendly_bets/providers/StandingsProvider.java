package net.friendly_bets.providers;

import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;

import java.util.List;

/**
 * Layer STANDINGS: fetch league table from external source and persist snapshot in {@code standings}.
 */
public interface StandingsProvider extends ExternalDataProvider {

    StandingsSyncResultDto syncByLeagueCode(String leagueCode);

    StandingsSyncResultDto syncLeague(Season season, League league);

    /**
     * Provider-specific fetch + parse by league code (sandbox / alias autobind). No DB writes.
     */
    StandingsTableSnapshot fetchTableSnapshotByLeagueCode(String leagueCode);

    /**
     * Provider-specific fetch + parse. Persistence is handled by {@link StandingsPersistSupport}.
     */
    default StandingsTableSnapshot fetchTableSnapshot(Season season, League league) {
        if (league == null || league.getLeagueCode() == null) {
            throw new BadRequestException("leagueCodeRequired");
        }
        return fetchTableSnapshotByLeagueCode(league.getLeagueCode().name());
    }

    default List<String> fetchTeamNames(String leagueCode) {
        StandingsTableSnapshot snapshot = fetchTableSnapshotByLeagueCode(leagueCode);
        List<String> names = snapshot.getRows() == null
                ? List.of()
                : snapshot.getRows().stream()
                .map(StandingRowSnapshot::getExternalTeamName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) {
            throw new BadRequestException("standingsTeamNamesEmpty");
        }
        return names;
    }
}
