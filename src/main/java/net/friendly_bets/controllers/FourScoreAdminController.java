package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.fourscore.FourScorePreviewMatchDto;
import net.friendly_bets.fourscore.FourScoreSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/fourscore")
public class FourScoreAdminController {

    private final FourScoreSyncService fourScoreSyncService;

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<FourScorePreviewMatchDto>> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fourScoreSyncService.previewDate(date));
    }

    @PostMapping("/sync/{leagueCode}/matchdays/{matchday}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<Integer> syncMatchday(
            @PathVariable String leagueCode,
            @PathVariable int matchday,
            @RequestParam String season,
            @RequestParam(required = false) String leagueId
    ) {
        int updated = fourScoreSyncService.syncMatchday(leagueCode, matchday, season, leagueId);
        return ResponseEntity.ok(updated);
    }
}
