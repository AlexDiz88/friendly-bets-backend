package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MatchResultSyncSettingsDto;
import net.friendly_bets.dto.PatchMatchResultSyncSettingsDto;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/match-result-sync-settings")
public class MatchResultSyncSettingsController {

    private final MatchResultSyncSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchResultSyncSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.toDto());
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<MatchResultSyncSettingsDto> patchSettings(
            @Valid @RequestBody PatchMatchResultSyncSettingsDto body
    ) {
        return ResponseEntity.ok(settingsService.patch(body));
    }
}
