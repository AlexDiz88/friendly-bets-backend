package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.oddsapi.OddsApiSyncResult;
import net.friendly_bets.oddsapi.OddsApiSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/odds")
public class OddsApiAdminController {

    private final OddsApiSyncService oddsApiSyncService;

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> syncNow() {
        OddsApiSyncResult result = oddsApiSyncService.runTick();
        return ResponseEntity.ok(Map.of(
                "message", "oddsApiSyncCompleted",
                "leaguesProcessed", result.getLeaguesProcessed(),
                "matchesEligible", result.getMatchesEligible(),
                "oddsDocumentsSaved", result.getOddsDocumentsSaved(),
                "matchesSkippedStarted", result.getMatchesSkippedStarted(),
                "mappingFailures", result.getMappingFailures()
        ));
    }
}
