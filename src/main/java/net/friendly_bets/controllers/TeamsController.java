package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.TeamsApi;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.services.TeamsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamsController implements TeamsApi {

    private final TeamsService teamsService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<TeamsPage> getTeams() {
        return ResponseEntity.ok(teamsService.getAll());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{league-id}")
    public ResponseEntity<TeamsPage> getLeagueTeams(@PathVariable("league-id") String leagueId) {
        return ResponseEntity.ok(teamsService.getLeagueTeams(leagueId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<TeamDto> createTeam(@RequestBody @Valid NewTeamDto newTeam) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamsService.createTeam(newTeam));
    }

}
