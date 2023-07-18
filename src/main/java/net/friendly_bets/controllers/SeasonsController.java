package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.SeasonsApi;
import net.friendly_bets.dto.*;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.SeasonsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seasons")
public class SeasonsController implements SeasonsApi {

    private final SeasonsService seasonsService;

    @Override
    @GetMapping
    public ResponseEntity<SeasonsPage> getSeasons(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(seasonsService.getAll());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<SeasonDto> addSeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @RequestBody NewSeasonDto newSeason) {
        return ResponseEntity.status(201)
                .body(seasonsService.addSeason(newSeason));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<SeasonDto> changeSeasonStatus(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable("id") String id,
                                                        @RequestBody String status) {
        return ResponseEntity
                .ok(seasonsService.changeSeasonStatus(id, status));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getSeasonStatusList(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(seasonsService.getSeasonStatusList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public ResponseEntity<SeasonDto> getActiveSeason(AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(seasonsService.getActiveSeason());

    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/scheduled")
    public ResponseEntity<SeasonDto> getScheduledSeason(AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(seasonsService.getScheduledSeason());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/registration/{season-id}")
    public ResponseEntity<SeasonDto> registrationInSeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable("season-id") String seasonId) {
        String userId = currentUser.getUser().getId();
        return ResponseEntity
                .ok(seasonsService.registrationInSeason(userId, seasonId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{season-id}/leagues")
    public ResponseEntity<LeaguesPage> getLeaguesBySeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                          @PathVariable("season-id") String seasonId) {
        return ResponseEntity
                .ok(seasonsService.getLeaguesBySeason(seasonId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{season-id}/leagues")
    public ResponseEntity<LeagueDto> addLeagueToSeason(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @PathVariable("season-id") String seasonId,
                                                       @RequestBody NewLeagueDto newLeague) {
        return ResponseEntity.status(201)
                .body(seasonsService.addLeagueToSeason(seasonId, newLeague));
    }


}
