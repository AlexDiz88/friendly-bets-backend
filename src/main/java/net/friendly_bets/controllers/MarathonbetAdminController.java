package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.MarathonbetSyncResult;
import net.friendly_bets.marathonbet.MarathonbetSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/marathonbet")
public class MarathonbetAdminController {

    private final MarathonbetSyncService marathonbetSyncService;

    @PostMapping("/sync-slot")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> syncSlot(
            @RequestParam String leagueId,
            @RequestParam int matchday,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) List<String> matchScheduleIds
    ) {
        MarathonbetSyncResult result = marathonbetSyncService.syncSlot(
                leagueId, matchday, season, matchScheduleIds);
        return ResponseEntity.ok(Map.of(
                "message", "marathonbetSyncCompleted",
                "tournamentFetched", result.isTournamentFetched(),
                "matchesEligible", result.getMatchesEligible(),
                "matchesMatched", result.getMatchesMatched(),
                "mergedSaved", result.getMergedSaved(),
                "sseCalls", result.getSseCalls(),
                "mappingFailures", result.getMappingFailures(),
                "failedMatchScheduleIds", result.getFailedMatchScheduleIds()
        ));
    }
}
