package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalDataSandboxFullMatchRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxLiveRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxOddsRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxResultDto;
import net.friendly_bets.dto.ExternalDataSandboxScheduleRequestDto;
import net.friendly_bets.dto.ExternalDataSandboxStandingsRequestDto;
import net.friendly_bets.services.ExternalDataSandboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-data/sandbox")
public class ExternalDataSandboxAdminController {

    private final ExternalDataSandboxService sandboxService;

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataSandboxResultDto> schedule(
            @RequestBody ExternalDataSandboxScheduleRequestDto body
    ) {
        return ResponseEntity.ok(sandboxService.runSchedule(body));
    }

    @PostMapping("/odds")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataSandboxResultDto> odds(
            @RequestBody ExternalDataSandboxOddsRequestDto body
    ) {
        return ResponseEntity.ok(sandboxService.runOdds(body));
    }

    @PostMapping("/live")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataSandboxResultDto> live(
            @RequestBody ExternalDataSandboxLiveRequestDto body
    ) {
        return ResponseEntity.ok(sandboxService.runLive(body));
    }

    @PostMapping("/full-match")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataSandboxResultDto> fullMatch(
            @RequestBody ExternalDataSandboxFullMatchRequestDto body
    ) {
        return ResponseEntity.ok(sandboxService.runFullMatch(body));
    }

    @PostMapping("/standings")
    @PreAuthorize("hasAnyAuthority('ADMIN','MODERATOR')")
    public ResponseEntity<ExternalDataSandboxResultDto> standings(
            @RequestBody ExternalDataSandboxStandingsRequestDto body
    ) {
        return ResponseEntity.ok(sandboxService.runStandings(body));
    }
}
