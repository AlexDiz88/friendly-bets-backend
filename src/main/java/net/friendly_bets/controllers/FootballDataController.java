package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.ExternalMatchdayPageDto;
import net.friendly_bets.dto.ExternalMatchdaySyncDto;
import net.friendly_bets.footballdata.AutoBetSettlementService;
import net.friendly_bets.footballdata.AutoSettleResult;
import net.friendly_bets.footballdata.ExternalMatchDisplayService;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.models.Season;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.external.ExternalMatch;
import net.friendly_bets.models.external.ExternalMatchdaySync;
import net.friendly_bets.repositories.ExternalMatchdaySyncRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/football-data")
public class FootballDataController {

    private final FootballDataSyncService footballDataSyncService;
    private final ExternalMatchDisplayService externalMatchDisplayService;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final ExternalMatchdaySyncRepository externalMatchdaySyncRepository;
    private final FootballDataProperties footballDataProperties;
    private final AutoBetSettlementService autoBetSettlementService;
    private final SeasonsRepository seasonsRepository;

    @GetMapping("/competitions/{competitionCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalCompetitionInfoDto> getCompetitionInfo(
            @PathVariable String competitionCode,
            @RequestParam(defaultValue = "2025") String season) {
        return ResponseEntity.ok(footballDataCompetitionService.getCompetitionInfo(competitionCode, season));
    }

    @GetMapping("/competitions/{competitionCode}/matchdays/{matchday}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalMatchdayPageDto> getMatchday(
            @PathVariable String competitionCode,
            @PathVariable int matchday,
            @RequestParam(defaultValue = "2025") String season) {

        ExternalMatchdaySyncDto syncDto = externalMatchdaySyncRepository
                .findByCompetitionCodeAndMatchdayAndSeason(competitionCode, matchday, season)
                .map(ExternalMatchdaySyncDto::from)
                .orElse(null);

        List<ExternalMatch> matches = footballDataSyncService.getMatches(competitionCode, matchday, season);

        return ResponseEntity.ok(ExternalMatchdayPageDto.builder()
                .sync(syncDto)
                .matches(externalMatchDisplayService.toDisplayDtos(matches))
                .build());
    }

    @PostMapping("/competitions/{competitionCode}/matchdays/{matchday}/sync")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<ExternalMatchdaySyncDto> syncMatchday(
            @PathVariable String competitionCode,
            @PathVariable int matchday,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String leagueId) {

        String resolvedSeason = season != null ? season : footballDataProperties.getDefaultSeason();
        ExternalMatchdaySync sync = footballDataSyncService.syncMatchday(
                competitionCode, matchday, resolvedSeason, leagueId);
        autoBetSettlementService.settleActiveSeasonIfEnabled();
        return ResponseEntity.ok(ExternalMatchdaySyncDto.from(sync));
    }

    @PostMapping("/seasons/{seasonId}/refresh")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> refreshSeason(@PathVariable String seasonId) {
        footballDataSyncService.refreshSeason(seasonId);
        seasonsRepository.findById(seasonId)
                .ifPresent(autoBetSettlementService::settleSeason);
        return ResponseEntity.ok(Map.of(
                "message", "footballDataRefreshStarted",
                "seasonId", seasonId
        ));
    }

    @PostMapping("/seasons/{seasonId}/auto-settle")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<AutoSettleResult> autoSettleSeason(@PathVariable String seasonId) {
        Season season = seasonsRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("Season", seasonId));
        return ResponseEntity.ok(autoBetSettlementService.settleSeason(season));
    }

    @GetMapping("/seasons/{seasonId}/cached-game-results")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<GameResult>> getCachedGameResults(@PathVariable String seasonId) {
        return ResponseEntity.ok(footballDataSyncService.getCachedGameResultsForSeason(seasonId));
    }
}
