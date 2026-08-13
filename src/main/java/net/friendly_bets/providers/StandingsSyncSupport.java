package net.friendly_bets.providers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.providers.standings.StandingsLeagueCodes;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider-agnostic STANDINGS sync: resolve running season/league, monitor, persist snapshot.
 */
@Component
@RequiredArgsConstructor
public class StandingsSyncSupport {

    private final StandingsPersistSupport persistSupport;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalApiMonitoringService monitoringService;

    public StandingsSyncResultDto syncByLeagueCode(StandingsProvider provider, String leagueCodeRaw) {
        League.LeagueCode leagueCode = StandingsLeagueCodes.parse(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = StandingsLeagueCodes.findLeague(season, leagueCode)
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        return syncLeague(provider, season, league);
    }

    public StandingsSyncResultDto syncLeague(StandingsProvider provider, Season season, League league) {
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.STANDINGS,
                provider.providerId(),
                ExternalApiMonitoringTrigger.ORCHESTRATOR,
                league.getLeagueCode() != null ? league.getLeagueCode().name() : null,
                season.getId()
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        try {
            StandingsTableSnapshot snapshot = provider.fetchTableSnapshot(season, league);
            StandingsSyncResultDto result = persistSupport.persist(snapshot, provider.providerId(), season, league);
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
}
