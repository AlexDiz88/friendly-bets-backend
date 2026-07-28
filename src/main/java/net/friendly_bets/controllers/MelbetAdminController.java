package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.melbet.MelbetSyncResult;
import net.friendly_bets.melbet.MelbetSyncService;
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
@RequestMapping("/api/admin/melbet")
public class MelbetAdminController {

    private final MelbetSyncService melbetSyncService;

    /**
     * Manual ODDS sync. Default: current matchday, GetEvent only for matches without odds.
     * With {@code force=true}: require matchday + matchScheduleIds and re-fetch even if odds exist.
     */
    @PostMapping("/sync-slot")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> syncSlot(
            @RequestParam String leagueId,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(required = false) Integer matchday,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) List<String> matchScheduleIds
    ) {
        MelbetSyncResult result = melbetSyncService.syncSlot(
                leagueId, season, force, matchday, matchScheduleIds);
        return ResponseEntity.ok(Map.of(
                "message", "melbetSyncCompleted",
                "tournamentFetched", result.isTournamentFetched(),
                "matchesEligible", result.getMatchesEligible(),
                "matchesMatched", result.getMatchesMatched(),
                "mergedSaved", result.getMergedSaved(),
                "sseCalls", result.getEventCalls(),
                "mappingFailures", result.getMappingFailures(),
                "skippedFar", result.getSkippedFar(),
                "skippedNoBookieEvent", result.getSkippedNoBookieEvent(),
                "failedMatchScheduleIds", result.getFailedMatchScheduleIds()
        ));
    }
}
