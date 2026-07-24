package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdayPageDto;
import net.friendly_bets.dto.ExternalMatchdaySyncDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.gameresults.ExternalCompetitionService;
import net.friendly_bets.gameresults.GameResultCollector;
import net.friendly_bets.gameresults.GameResultDisplayService;
import net.friendly_bets.gameresults.GameResultQueryService;
import net.friendly_bets.gameresults.LeagueCodePathSupport;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match-results")
public class MatchResultsController {

    private final GameResultQueryService gameResultQueryService;
    private final GameResultDisplayService gameResultDisplayService;
    private final ExternalCompetitionService externalCompetitionService;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final SeasonsRepository seasonsRepository;
    private final GameResultCollector gameResultCollector;

    @GetMapping("/competitions/{pathLeagueOrCompetitionCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalCompetitionInfoDto> getCompetitionInfo(
            @PathVariable String pathLeagueOrCompetitionCode,
            @RequestParam(defaultValue = "2025") String season) {
        String externalCode = LeagueCodePathSupport.toExternalCompetitionCode(
                LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode));
        return ResponseEntity.ok(externalCompetitionService.getCompetitionInfo(externalCode, season));
    }

    @GetMapping("/competitions/{pathLeagueOrCompetitionCode}/matchdays/{matchday}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalMatchdayPageDto> getMatchday(
            @PathVariable String pathLeagueOrCompetitionCode,
            @PathVariable int matchday,
            @RequestParam(defaultValue = "2025") String season,
            @RequestParam(required = false) String leagueId) {

        String leagueCode = LeagueCodePathSupport.resolveStorageLeagueCode(pathLeagueOrCompetitionCode);

        ExternalMatchdaySyncDto syncDto = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season)
                .map(ExternalMatchdaySyncDto::from)
                .orElse(null);

        var matches = gameResultQueryService.getMatches(
                pathLeagueOrCompetitionCode, matchday, season, leagueId);

        return ResponseEntity.ok(ExternalMatchdayPageDto.builder()
                .sync(syncDto)
                .matches(gameResultDisplayService.toDisplayDtos(matches))
                .build());
    }

    @GetMapping("/seasons/{seasonId}/cached-game-results")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<GameResult>> getCachedGameResults(@PathVariable String seasonId) {
        Season season = seasonsRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("Season", seasonId));
        return ResponseEntity.ok(gameResultCollector.collectForSeason(season));
    }
}
