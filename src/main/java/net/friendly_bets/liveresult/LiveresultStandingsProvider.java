package net.friendly_bets.liveresult;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.StandingsProvider;
import net.friendly_bets.providers.StandingsSyncSupport;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class LiveresultStandingsProvider implements StandingsProvider {

    private final LiveresultTeamNamesService teamNamesService;
    private final StandingsSyncSupport syncSupport;

    @Override
    public String providerId() {
        return ExternalProviderIds.LIVERESULT;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.STANDINGS);
    }

    @Override
    public StandingsSyncResultDto syncByLeagueCode(String leagueCodeRaw) {
        return syncSupport.syncByLeagueCode(this, leagueCodeRaw);
    }

    @Override
    public StandingsSyncResultDto syncLeague(Season season, League league) {
        return syncSupport.syncLeague(this, season, league);
    }

    @Override
    public StandingsTableSnapshot fetchTableSnapshotByLeagueCode(String leagueCodeRaw) {
        return teamNamesService.fetchStandingsSnapshot(leagueCodeRaw);
    }
}
