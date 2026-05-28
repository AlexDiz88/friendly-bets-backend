package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
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
@RequestMapping("/api/admin/api-sync-issues")
public class ApiSyncIssuesController {

    private final ApiSyncIssueService apiSyncIssueService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<ApiSyncIssue>> getLatest() {
        return ResponseEntity.ok(apiSyncIssueService.getLatest());
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("hasIssues", apiSyncIssueService.hasIssues()));
    }

    @GetMapping("/unmapped-team-names")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<UnmappedExternalTeamNameDto>> getUnmappedTeamNames() {
        return ResponseEntity.ok(apiSyncIssueService.getUnmappedTeamNameHints());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> clearAll() {
        apiSyncIssueService.clearAll();
        return ResponseEntity.ok(Map.of("message", "apiSyncIssuesCleared"));
    }
}
