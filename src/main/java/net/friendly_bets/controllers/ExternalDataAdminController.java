package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalDataLayerConfigDto;
import net.friendly_bets.dto.ExternalSiteAccessProbeRequestDto;
import net.friendly_bets.dto.ExternalSiteAccessProbeResultDto;
import net.friendly_bets.dto.LiveMatchSyncResultDto;
import net.friendly_bets.dto.ScheduleSyncResultDto;
import net.friendly_bets.dto.StandingsSyncResultDto;
import net.friendly_bets.dto.ExternalTeamNamesLoadResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.LayerProviderRegistry;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.OddsProvider;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.providers.StandingsProvider;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.ExternalSiteAccessProbeService;
import net.friendly_bets.services.ExternalTeamNamesService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.MatchFinalizeOrchestrator;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-data")
public class ExternalDataAdminController {

    private final ExternalDataLayerConfigService configService;
    private final LayerProviderRouter router;
    private final LayerProviderRegistry layerProviderRegistry;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalTeamNamesService externalTeamNamesService;
    private final ExternalSiteAccessProbeService siteAccessProbeService;

    @PostMapping("/site-access-probe")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ExternalSiteAccessProbeResultDto> siteAccessProbe(
            @RequestBody ExternalSiteAccessProbeRequestDto body
    ) {
        return ResponseEntity.ok(siteAccessProbeService.probe(body != null ? body.getUrl() : null));
    }

    @GetMapping("/layers")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataLayerConfigDto> getLayers() {
        return ResponseEntity.ok(ExternalDataLayerConfigDto.from(
                configService.getOrCreateDefaults(),
                configService.capabilitiesCatalog()
        ));
    }

    @PatchMapping("/layers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ExternalDataLayerConfigDto> patchLayers(@RequestBody ExternalDataLayerConfigDto body) {
        return ResponseEntity.ok(ExternalDataLayerConfigDto.from(
                configService.update(
                        body != null ? body.toEntityLayers() : null,
                        body != null ? body.getOddsRefreshWithinHours() : null
                ),
                configService.capabilitiesCatalog()
        ));
    }

    @PostMapping("/team-names")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ExternalTeamNamesLoadResultDto> fetchTeamNames(
            @RequestParam String provider,
            @RequestParam String leagueCode,
            @RequestParam(defaultValue = "false") boolean forceOverwrite
    ) {
        return ResponseEntity.ok(
                externalTeamNamesService.fetchAndAutoBindTeamNames(provider, leagueCode, forceOverwrite));
    }

    @PostMapping("/schedule/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ScheduleSyncResultDto> syncSchedule(
            @RequestParam String leagueCode,
            @RequestParam(required = false) Integer matchday
    ) {
        ScheduleSyncResultDto result = router.execute(
                ExternalDataLayer.SCHEDULE,
                ScheduleProvider.class,
                p -> p.syncByLeagueCode(leagueCode, matchday),
                leagueCode
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/odds/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OddsProvider.OddsSyncResult> syncOdds(@RequestParam String leagueCode) {
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = season.getLeagues().stream()
                .filter(l -> l != null && l.getLeagueCode() != null
                        && l.getLeagueCode().name().equalsIgnoreCase(leagueCode.trim()))
                .findFirst()
                .orElseThrow(() -> new net.friendly_bets.exceptions.BadRequestException("leagueNotFoundInSeason"));
        ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ADMIN);
        try {
            OddsProvider.OddsSyncResult result = router.execute(
                    ExternalDataLayer.ODDS,
                    OddsProvider.class,
                    p -> p.syncLeagueSlot(season, league, league.getCurrentMatchDay(), List.of()),
                    leagueCode
            );
            return ResponseEntity.ok(result);
        } finally {
            ExternalApiMonitoringService.clearTriggerOverride();
        }
    }

    @PostMapping("/live/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LiveMatchSyncResultDto> syncLive(
            @RequestParam String provider,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (provider == null || provider.isBlank()) {
            throw new net.friendly_bets.exceptions.BadRequestException("liveSyncProviderRequired");
        }
        if (date == null) {
            throw new net.friendly_bets.exceptions.BadRequestException("liveSyncDateRequired");
        }
        LiveMatchProvider liveProvider = layerProviderRegistry
                .findAs(provider.trim(), LiveMatchProvider.class)
                .filter(p -> p.supports(ExternalDataLayer.LIVE))
                .orElseThrow(() -> new net.friendly_bets.exceptions.BadRequestException(
                        "externalDataProviderUnavailable"));
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ADMIN);
        try {
            LiveMatchProvider.LiveSyncResult result = liveProvider.syncLive(season, date);
            try {
                ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ADMIN);
                matchFinalizeOrchestrator.finalizePendingFullMatches(result.pendingFullMatchIds());
            } catch (RuntimeException ignored) {
                // FULL failures are already in error_logs via LayerProviderRouter
            }
            return ResponseEntity.ok(LiveMatchSyncResultDto.from(result));
        } finally {
            ExternalApiMonitoringService.clearTriggerOverride();
        }
    }

    @PostMapping("/full-match/{matchScheduleId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchSchedule> fetchFullMatch(@PathVariable String matchScheduleId) {
        MatchSchedule match = matchScheduleRepository.findById(matchScheduleId)
                .orElseThrow(() -> new net.friendly_bets.exceptions.NotFoundException("MatchSchedule", matchScheduleId));
        ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ADMIN);
        try {
            MatchSchedule updated = router.execute(
                    ExternalDataLayer.FULL_MATCH,
                    FullMatchProvider.class,
                    p -> p.fetchAndPersistFullDetails(match)
            );
            return ResponseEntity.ok(updated);
        } finally {
            ExternalApiMonitoringService.clearTriggerOverride();
        }
    }

    @PostMapping("/standings/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StandingsSyncResultDto> syncStandings(@RequestParam String leagueCode) {
        ExternalApiMonitoringService.setTriggerOverride(ExternalApiMonitoringTrigger.ADMIN);
        try {
            StandingsSyncResultDto result = router.execute(
                    ExternalDataLayer.STANDINGS,
                    StandingsProvider.class,
                    p -> p.syncByLeagueCode(leagueCode),
                    leagueCode
            );
            return ResponseEntity.ok(result);
        } finally {
            ExternalApiMonitoringService.clearTriggerOverride();
        }
    }
}
