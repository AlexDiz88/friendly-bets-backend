package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.footballdata.ExternalSyncIssueService;
import net.friendly_bets.models.external.ExternalSyncIssue;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-sync-issues")
public class ExternalSyncIssuesController {

    private final ExternalSyncIssueService externalSyncIssueService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<ExternalSyncIssue>> getLatest() {
        return ResponseEntity.ok(externalSyncIssueService.getLatest());
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("hasIssues", externalSyncIssueService.hasIssues()));
    }

    @GetMapping("/unmapped-team-names")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UnmappedExternalTeamNameDto>> getUnmappedTeamNames() {
        return ResponseEntity.ok(externalSyncIssueService.getUnmappedTeamNameHints());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> clearAll() {
        externalSyncIssueService.clearAll();
        return ResponseEntity.ok(Map.of("message", "externalSyncIssuesCleared"));
    }
}

