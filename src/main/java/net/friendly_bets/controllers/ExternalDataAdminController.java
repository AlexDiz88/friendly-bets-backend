package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalDataLayerConfigDto;
import net.friendly_bets.dto.LiveMatchSyncResultDto;
import net.friendly_bets.dto.Soccer365ScheduleSyncResultDto;
import net.friendly_bets.dto.Soccer365TeamNameChipDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.OddsProvider;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.ExternalTeamNamesService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.MatchFinalizeOrchestrator;
import net.friendly_bets.services.RunningSeasonLookup;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-data")
public class ExternalDataAdminController {

    private final ExternalDataLayerConfigService configService;
    private final LayerProviderRouter router;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalTeamNamesService externalTeamNamesService;

    @GetMapping("/layers")
    @PreAuthorize("hasAuthority('ADMIN')")
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
                configService.update(body != null ? body.toEntityLayers() : null),
                configService.capabilitiesCatalog()
        ));
    }

    @PostMapping("/team-names")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Soccer365TeamNameChipDto>> fetchTeamNames(
            @RequestParam String provider,
            @RequestParam String leagueCode
    ) {
        return ResponseEntity.ok(externalTeamNamesService.fetchUnmappedTeamNames(provider, leagueCode));
    }

    @PostMapping("/schedule/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Soccer365ScheduleSyncResultDto> syncSchedule(@RequestParam String leagueCode) {
        Soccer365ScheduleSyncResultDto result = router.execute(
                ExternalDataLayer.SCHEDULE,
                ScheduleProvider.class,
                p -> p.syncByLeagueCode(leagueCode),
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
        OddsProvider.OddsSyncResult result = router.execute(
                ExternalDataLayer.ODDS,
                OddsProvider.class,
                p -> p.syncLeagueSlot(season, league, league.getCurrentMatchDay(), List.of()),
                leagueCode
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/live/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LiveMatchSyncResultDto> syncLive(@RequestParam String leagueCode) {
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = season.getLeagues().stream()
                .filter(l -> l != null && l.getLeagueCode() != null
                        && l.getLeagueCode().name().equalsIgnoreCase(leagueCode.trim()))
                .findFirst()
                .orElseThrow(() -> new net.friendly_bets.exceptions.BadRequestException("leagueNotFoundInSeason"));
        LiveMatchProvider.LiveSyncResult result = router.execute(
                ExternalDataLayer.LIVE,
                LiveMatchProvider.class,
                p -> p.syncLeagueLive(season, league),
                league.getLeagueCode().name()
        );
        try {
            matchFinalizeOrchestrator.finalizePendingFullMatches(result.pendingFullMatchIds());
        } catch (RuntimeException ignored) {
            // FULL failures are already in error_logs via LayerProviderRouter
        }
        return ResponseEntity.ok(LiveMatchSyncResultDto.from(result));
    }

    @PostMapping("/full-match/{matchScheduleId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchSchedule> fetchFullMatch(@PathVariable String matchScheduleId) {
        MatchSchedule match = matchScheduleRepository.findById(matchScheduleId)
                .orElseThrow(() -> new net.friendly_bets.exceptions.NotFoundException("MatchSchedule", matchScheduleId));
        MatchSchedule updated = router.execute(
                ExternalDataLayer.FULL_MATCH,
                FullMatchProvider.class,
                p -> p.fetchAndPersistFullDetails(match)
        );
        return ResponseEntity.ok(updated);
    }
}
