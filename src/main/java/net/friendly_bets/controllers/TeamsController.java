package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.TeamsApi;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.TeamsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamsController implements TeamsApi {

    private final TeamsService teamsService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<TeamsPage> getTeams(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(teamsService.getAll());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<TeamDto> createTeam(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @RequestBody NewTeamDto newTeam) {
        return ResponseEntity.status(201)
                .body(teamsService.createTeam(newTeam));
    }

}
