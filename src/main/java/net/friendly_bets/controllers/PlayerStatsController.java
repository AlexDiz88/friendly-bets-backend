package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.PlayerStatsApi;
import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsDto;
import net.friendly_bets.dto.PlayersStatsByLeaguesPage;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.PlayerStatsService;
import net.friendly_bets.services.UsersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class PlayerStatsController implements PlayerStatsApi {

    private final PlayerStatsService playerStatsService;
    @Override
    @GetMapping("/season/{season-id}")
    public ResponseEntity<AllPlayersStatsDto> getAllPlayersStatsBySeason(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(playerStatsService.getAllPlayersStatsBySeason(seasonId));
    }

    @Override
    @GetMapping("/season/{season-id}/leagues")
    public ResponseEntity<AllPlayersStatsByLeaguesDto> getAllPlayersStatsByLeagues(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.status(200)
                .body(playerStatsService.getAllPlayersStatsByLeagues(seasonId));
    }
}
