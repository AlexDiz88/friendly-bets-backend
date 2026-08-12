package net.friendly_bets.liveresult;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.liveresult.config.LiveresultProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.StandingsPersistSupport;
import net.friendly_bets.providers.StandingsProvider;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LiveresultStandingsProvider implements StandingsProvider {

    private final LiveresultProperties properties;
    private final LiveresultTeamNamesService teamNamesService;
    private final LiveresultHttpClient httpClient;
    private final LiveresultStandingsParser standingsParser;
    private final StandingsPersistSupport persistSupport;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalApiMonitoringService monitoringService;

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
        League.LeagueCode leagueCode = LiveresultTeamNamesService.parseLeagueCode(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = findLeague(season, leagueCode)
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        return syncLeague(season, league);
    }

    @Override
    public StandingsSyncResultDto syncLeague(Season season, League league) {
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.STANDINGS,
                ExternalProviderIds.LIVERESULT,
                ExternalApiMonitoringTrigger.ORCHESTRATOR,
                league.getLeagueCode() != null ? league.getLeagueCode().name() : null,
                season.getId()
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        try {
            StandingsTableSnapshot snapshot = fetchTableSnapshot(season, league);
            StandingsSyncResultDto result = persistSupport.persist(snapshot, providerId(), season, league);
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SUCCESS,
                    ExternalApiMonitoringCounters.builder()
                            .requested(1)
                            .saved(result.getRowsSaved())
                            .skipped(result.getSkippedUnmapped())
                            .build(),
                    httpLogs,
                    result.getUnmappedNames(),
                    null
            );
            return result;
        } catch (RuntimeException e) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().build(),
                    httpLogs,
                    List.of(),
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public StandingsTableSnapshot fetchTableSnapshot(Season season, League league) {
        if (league == null || league.getLeagueCode() == null) {
            throw new BadRequestException("leagueCodeRequired");
        }
        String path = teamNamesService.requireStandingsPath(league.getLeagueCode());
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        String url = base + (path.startsWith("/") ? path : "/" + path);
        String html = httpClient.fetchStandingsHtml(path);
        return standingsParser.parse(html, url);
    }

    private static java.util.Optional<League> findLeague(Season season, League.LeagueCode leagueCode) {
        if (season.getLeagues() == null) {
            return java.util.Optional.empty();
        }
        return season.getLeagues().stream()
                .filter(Objects::nonNull)
                .filter(l -> leagueCode.equals(l.getLeagueCode()))
                .findFirst();
    }
}
