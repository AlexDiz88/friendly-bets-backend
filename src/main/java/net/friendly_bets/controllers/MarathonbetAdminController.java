package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetSlotPreviewDto;
import net.friendly_bets.dto.MarathonbetSyncRunDto;
import net.friendly_bets.marathonbet.MarathonbetSlotPreviewService;
import net.friendly_bets.marathonbet.MarathonbetSyncResult;
import net.friendly_bets.marathonbet.MarathonbetSyncService;
import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final MarathonbetSlotPreviewService slotPreviewService;

    @GetMapping("/slot-preview")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<MarathonbetSlotPreviewDto> slotPreview(
            @RequestParam String leagueId,
            @RequestParam int matchday,
            @RequestParam(required = false) String season
    ) {
        return ResponseEntity.ok(slotPreviewService.buildPreview(leagueId, matchday, season));
    }

    @PostMapping("/sync-slot")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Map<String, Object>> syncSlot(
            @RequestParam String leagueId,
            @RequestParam int matchday,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) List<String> gameResultIds
    ) {
        MarathonbetSyncResult result = marathonbetSyncService.syncSlot(
                leagueId, matchday, season, gameResultIds);
        return ResponseEntity.ok(Map.of(
                "message", "marathonbetSyncCompleted",
                "tournamentFetched", result.isTournamentFetched(),
                "matchesEligible", result.getMatchesEligible(),
                "matchesMatched", result.getMatchesMatched(),
                "mergedSaved", result.getMergedSaved(),
                "sseCalls", result.getSseCalls(),
                "mappingFailures", result.getMappingFailures(),
                "failedGameResultIds", result.getFailedGameResultIds()
        ));
    }

    @GetMapping("/sync-runs/latest")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<MarathonbetSyncRunDto> latestRun() {
        return marathonbetSyncService.findLatestRun()
                .map(run -> ResponseEntity.ok(toDto(run)))
                .orElse(ResponseEntity.noContent().build());
    }

    private static MarathonbetSyncRunDto toDto(MarathonbetSyncRun run) {
        return MarathonbetSyncRunDto.builder()
                .id(run.getId())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .leagueCode(run.getLeagueCode())
                .season(run.getSeason())
                .slotOrders(run.getSlotOrders())
                .tournamentFetched(run.isTournamentFetched())
                .matchesEligible(run.getMatchesEligible())
                .matchesMatched(run.getMatchesMatched())
                .mergedSaved(run.getMergedSaved())
                .sseCalls(run.getSseCalls())
                .mappingFailures(run.getMappingFailures())
                .fallbackUsed(run.isFallbackUsed())
                .manual(run.isManual())
                .errorSummary(run.getErrorSummary())
                .build();
    }
}
