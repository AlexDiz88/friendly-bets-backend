package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.LeaguesApi;
import net.friendly_bets.dto.LeagueDto;
import net.friendly_bets.dto.LeaguesPage;
import net.friendly_bets.dto.NewLeagueDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leagues")
public class LeaguesController implements LeaguesApi {

//    private final LeaguesService leaguesService;

    @Override
    @GetMapping
    public ResponseEntity<LeaguesPage> getLeagues(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return null;
//        return ResponseEntity
//                .ok(leaguesService.getAll());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<LeagueDto> addLeague(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @RequestBody NewLeagueDto newLeague) {
        return null;
//        return ResponseEntity.status(201)
//                .body(leaguesService.addLeague(newLeague));
    }

}
