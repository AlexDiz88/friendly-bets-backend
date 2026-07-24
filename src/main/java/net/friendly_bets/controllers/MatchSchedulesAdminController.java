package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.GameResultsToMatchSchedulesMigrationResultDto;
import net.friendly_bets.dto.MatchScheduleBetsLinkResultDto;
import net.friendly_bets.services.GameResultsToMatchSchedulesMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/match-schedules")
public class MatchSchedulesAdminController {

    private final GameResultsToMatchSchedulesMigrationService migrationService;

    @PostMapping("/migrate-from-game-results")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<GameResultsToMatchSchedulesMigrationResultDto> migrateFromGameResults(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) String leagueCode
    ) {
        return ResponseEntity.ok(migrationService.migrate(seasonId, leagueCode));
    }

    @PostMapping("/link-bets")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchScheduleBetsLinkResultDto> linkBets(
            @RequestParam(required = false) String seasonId,
            @RequestParam(required = false) String leagueCode
    ) {
        return ResponseEntity.ok(migrationService.linkBetsToSchedules(seasonId, leagueCode));
    }
}
