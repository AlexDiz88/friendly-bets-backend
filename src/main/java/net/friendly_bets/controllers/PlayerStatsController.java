package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.PlayerStatsApi;
import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsPage;
import net.friendly_bets.dto.AllStatsByTeamsInSeasonDto;
import net.friendly_bets.dto.StatsByTeamsDto;
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
public class PlayerStatsController implements PlayerStatsApi {

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
}
