package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdayPageDto;
import net.friendly_bets.dto.ExternalMatchdaySyncDto;
import net.friendly_bets.footballdata.AutoBetSettlementService;
import net.friendly_bets.footballdata.AutoSettleResult;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.footballdata.GameResultDisplayService;
import net.friendly_bets.footballdata.LeagueCodePathSupport;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.repositories.SeasonsRepository;
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
    private final GameResultDisplayService gameResultDisplayService;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final FootballDataProperties footballDataProperties;
    private final AutoBetSettlementService autoBetSettlementService;
    private final SeasonsRepository seasonsRepository;

    @GetMapping("/competitions/{pathLeagueOrCompetitionCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalCompetitionInfoDto> getCompetitionInfo(
            @PathVariable String pathLeagueOrCompetitionCode,
            @RequestParam(defaultValue = "2025") String season) {
        String externalCode = LeagueCodePathSupport.toExternalCompetitionCode(
                LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode));
        return ResponseEntity.ok(footballDataCompetitionService.getCompetitionInfo(externalCode, season));
    }

    @GetMapping("/competitions/{pathLeagueOrCompetitionCode}/matchdays/{matchday}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalMatchdayPageDto> getMatchday(
            @PathVariable String pathLeagueOrCompetitionCode,
            @PathVariable int matchday,
            @RequestParam(defaultValue = "2025") String season) {

        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);

        ExternalMatchdaySyncDto syncDto = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season)
                .map(ExternalMatchdaySyncDto::from)
                .orElse(null);

        var matches = footballDataSyncService.getMatches(pathLeagueOrCompetitionCode, matchday, season);

        return ResponseEntity.ok(ExternalMatchdayPageDto.builder()
                .sync(syncDto)
                .matches(gameResultDisplayService.toDisplayDtos(matches))
                .build());
    }

    @PostMapping("/competitions/{pathLeagueOrCompetitionCode}/matchdays/{matchday}/sync")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<ExternalMatchdaySyncDto> syncMatchday(
            @PathVariable String pathLeagueOrCompetitionCode,
            @PathVariable int matchday,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String leagueId) {

        String resolvedSeason = season != null ? season : footballDataProperties.getDefaultSeason();
        GameResultsSync sync = footballDataSyncService.syncMatchday(
                pathLeagueOrCompetitionCode, matchday, resolvedSeason, leagueId);
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
