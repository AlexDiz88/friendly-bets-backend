package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.AssignTournamentFormatDto;
import net.friendly_bets.dto.SetLeagueCurrentMatchdayDto;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.LeagueDto;
import net.friendly_bets.dto.LeaguesWithoutFormatPage;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.services.LeaguesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leagues")
public class LeaguesController {

    private final FootballDataCompetitionService footballDataCompetitionService;
    private final LeaguesService leaguesService;

    @GetMapping("/without-tournament-format")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LeaguesWithoutFormatPage> getLeaguesWithoutFormat() {
        return ResponseEntity.ok(leaguesService.getLeaguesWithoutFormat());
    }

    @PatchMapping("/{leagueId}/tournament-format")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LeagueDto> assignTournamentFormat(
            @PathVariable String leagueId,
            @RequestBody @Valid AssignTournamentFormatDto body
    ) {
        return ResponseEntity.ok(
                leaguesService.assignTournamentFormat(leagueId, body.getTournamentFormatId())
        );
    }

    @PatchMapping("/{leagueId}/current-matchday")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<LeagueDto> setCurrentMatchday(
            @PathVariable String leagueId,
            @RequestBody @Valid SetLeagueCurrentMatchdayDto body
    ) {
        return ResponseEntity.ok(leaguesService.setCurrentMatchday(leagueId, body.getMatchDay()));
    }

    @GetMapping("/{leagueId}/external-competition-info")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalCompetitionInfoDto> getExternalCompetitionInfo(
            @PathVariable String leagueId,
            @RequestParam(defaultValue = "2025") String season
    ) {
        return ResponseEntity.ok(footballDataCompetitionService.getCompetitionInfoForLeague(leagueId, season));
    }
}
