package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.StatsApi;
import net.friendly_bets.dto.*;
import net.friendly_bets.services.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class StatsController implements StatsApi {

    private final StatsService statsService;

    @Override
    @GetMapping("/season/{season-id}")
    public ResponseEntity<AllPlayersStatsPage> getAllPlayersStatsBySeason(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(statsService.getAllPlayersStatsBySeason(seasonId));
    }

    @Override
    @GetMapping("/season/{season-id}/leagues")
    public ResponseEntity<AllPlayersStatsByLeaguesDto> getAllPlayersStatsByLeagues(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(statsService.getAllPlayersStatsByLeagues(seasonId));
    }

    @Override
    @GetMapping("/season/{season-id}/teams")
    public ResponseEntity<AllStatsByTeamsInSeasonDto> getAllStatsByTeamsInSeason(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(statsService.getAllStatsByTeamsInSeason(seasonId));
    }

    @Override
    @GetMapping("/season/{season-id}/bet-titles")
    public ResponseEntity<AllStatsByBetTitlesInSeasonDto> getAllStatsByBetTitlesInSeason(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(statsService.getAllStatsByBetTitlesInSeason(seasonId));
    }

    @Override
    @GetMapping("/season/{season-id}/league/{league-id}/user/{user-id}")
    public ResponseEntity<StatsByTeamsDto> getStatsByTeams(@PathVariable("season-id") String seasonId,
                                                           @PathVariable("league-id") String leagueId,
                                                           @PathVariable("user-id") String userId) {
        return ResponseEntity.status(200)
                .body(statsService.getStatsByTeams(seasonId, leagueId, userId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/season/{season-id}/recalculation")
    public ResponseEntity<AllPlayersStatsPage> playersStatsFullRecalculation(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(statsService.playersStatsFullRecalculation(seasonId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/season/{season-id}/recalculation/teams")
    public ResponseEntity<Void> playersStatsByTeamsRecalculation(@PathVariable("season-id") String seasonId) {
        statsService.playersStatsByTeamsRecalculation(seasonId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/season/{season-id}/recalculation/bet-titles")
    public ResponseEntity<Void> playersStatsByBetTitlesRecalculation(@PathVariable("season-id") String seasonId) {
        statsService.playersStatsByBetTitlesRecalculation(seasonId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/season/{season-id}/recalculation/gameweeks")
    public ResponseEntity<Void> recalculateAllGameweekStats(@PathVariable("season-id") String seasonId) {
        statsService.recalculateAllGameweekStats(seasonId);
        return ResponseEntity.ok().build();
    }
}
