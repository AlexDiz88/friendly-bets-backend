package net.friendly_bets.flashscoreua;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.flashscoreua.config.FlashscoreUaProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.StandingsProvider;
import net.friendly_bets.providers.StandingsSyncSupport;
import net.friendly_bets.providers.standings.StandingsLeagueCodes;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlashscoreUaStandingsProvider implements StandingsProvider {

    private final FlashscoreUaProperties properties;
    private final FlashscoreUaHttpClient httpClient;
    private final FlashscoreUaStandingsParser standingsParser;
    private final StandingsSyncSupport syncSupport;

    @Override
    public String providerId() {
        return ExternalProviderIds.FLASHSCORE_UA;
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
        League.LeagueCode leagueCode = StandingsLeagueCodes.parse(leagueCodeRaw);
        FlashscoreUaProperties.LeagueConfig config = requireLeagueConfig(leagueCode);
        String feed = httpClient.fetchOverallTableFeed(config);
        return standingsParser.parse(feed, sourceUrl(config));
    }

    FlashscoreUaProperties.LeagueConfig requireLeagueConfig(League.LeagueCode leagueCode) {
        FlashscoreUaProperties.LeagueConfig config = properties.getLeagues() != null
                ? properties.getLeagues().get(leagueCode.name())
                : null;
        if (config == null
                || config.getTournamentId() == null || config.getTournamentId().isBlank()
                || config.getStageId() == null || config.getStageId().isBlank()) {
            throw new BadRequestException("flashscoreUaStandingsNotConfigured");
        }
        return config;
    }

    private String sourceUrl(FlashscoreUaProperties.LeagueConfig config) {
        String base = properties.getBaseUrl() == null ? "https://www.flashscore.com.ua" : properties.getBaseUrl();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = config.getTournamentPath() == null || config.getTournamentPath().isBlank()
                ? "/"
                : config.getTournamentPath().trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return base + path + "#/" + config.getStageId().trim() + "/standings/overall/";
    }
}
