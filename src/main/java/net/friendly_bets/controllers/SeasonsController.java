package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.SeasonsApi;
import net.friendly_bets.dto.NewSeasonDto;
import net.friendly_bets.dto.SeasonDto;
import net.friendly_bets.dto.SeasonsPage;
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
    @PatchMapping("/{title}")
    public ResponseEntity<SeasonDto> changeSeasonStatus(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable("title") String title,
                                                        @RequestBody String status) {
        return ResponseEntity.ok(seasonsService.changeSeasonStatus(title, status));
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
        SeasonDto activeSeason = seasonsService.getActiveSeason();
        if (activeSeason == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(activeSeason);

    }
}
