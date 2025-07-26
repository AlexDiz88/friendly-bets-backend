package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.SeasonsApi;
import net.friendly_bets.dto.*;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.SeasonsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seasons")
public class SeasonsController implements SeasonsApi {

    private final SeasonsService seasonsService;

    @Override
    @GetMapping
    public ResponseEntity<SeasonsPage> getSeasons() {
        return ResponseEntity.ok(seasonsService.getAll());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<SeasonDto> addSeason(@RequestBody @Valid NewSeasonDto newSeason) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seasonsService.addSeason(newSeason));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<SeasonDto> changeSeasonStatus(@PathVariable("id") String id,
                                                        @RequestBody String status) {
        return ResponseEntity.ok(seasonsService.changeSeasonStatus(id, status));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getSeasonStatusList() {
        return ResponseEntity.ok(seasonsService.getSeasonStatusList());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/leagues/codes")
    public ResponseEntity<List<String>> getLeagueCodeList() {
        return ResponseEntity.ok(seasonsService.getLeagueCodeList());
    }

    @Override
    @GetMapping("/active")
    public ResponseEntity<SeasonDto> getActiveSeason() {
        return ResponseEntity.ok(seasonsService.getActiveSeason());
    }

    @Override
    @GetMapping("/active/id")
    public ResponseEntity<ActiveSeasonIdDto> getActiveSeasonId() {
        return ResponseEntity.ok(seasonsService.getActiveSeasonId());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/scheduled")
    public ResponseEntity<SeasonDto> getScheduledSeason() {
        return ResponseEntity.ok(seasonsService.getScheduledSeason());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/registration/{season-id}")
    public ResponseEntity<SeasonDto> registrationInSeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                          @PathVariable("season-id") String seasonId) {
        String userId = currentUser.getUser().getId();
        return ResponseEntity.ok(seasonsService.registrationInSeason(userId, seasonId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{season-id}/leagues")
    public ResponseEntity<LeaguesPage> getLeaguesBySeason(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.ok(seasonsService.getLeaguesBySeason(seasonId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{season-id}/leagues")
    public ResponseEntity<SeasonDto> addLeagueToSeason(@PathVariable("season-id") String seasonId,
                                                       @RequestBody @Valid NewLeagueDto newLeague) {
        return ResponseEntity.ok(seasonsService.addLeagueToSeason(seasonId, newLeague));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{season-id}/leagues/{league-id}/teams/{team-id}")
    public ResponseEntity<TeamDto> addTeamToLeagueInSeason(@PathVariable("season-id") String seasonId,
                                                           @PathVariable("league-id") String leagueId,
                                                           @PathVariable("team-id") String teamId) {
        return ResponseEntity.ok(seasonsService.addTeamToLeagueInSeason(seasonId, leagueId, teamId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/db-update")
    public ResponseEntity<Map<String, Object>> dbUpdate() {
        return ResponseEntity.ok(seasonsService.dbUpdate());
    }
}
