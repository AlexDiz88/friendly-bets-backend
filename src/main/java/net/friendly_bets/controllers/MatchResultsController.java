package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdayPageDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.gameresults.ExternalCompetitionService;
import net.friendly_bets.gameresults.GameResultCollector;
import net.friendly_bets.gameresults.LeagueCodePathSupport;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match-results")
public class MatchResultsController {

    private final MatchScheduleQueryService matchScheduleQueryService;
    private final MatchScheduleDisplayService matchScheduleDisplayService;
    private final ExternalCompetitionService externalCompetitionService;
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

        List<MatchSchedule> matches = matchScheduleQueryService.getMatches(
                pathLeagueOrCompetitionCode, matchday, season, leagueId);

        return ResponseEntity.ok(ExternalMatchdayPageDto.builder()
                .sync(null)
                .matches(matchScheduleDisplayService.toDisplayDtos(matches, season))
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
