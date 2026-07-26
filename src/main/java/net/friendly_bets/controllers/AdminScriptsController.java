package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.UtcTimestampsMigrationResultDto;
import net.friendly_bets.services.UtcTimestampsMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/scripts")
public class AdminScriptsController {

    private final UtcTimestampsMigrationService utcTimestampsMigrationService;

    @PostMapping("/migrate-timestamps-to-utc-instant")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<UtcTimestampsMigrationResultDto> migrateTimestampsToUtcInstant() {
        return ResponseEntity.ok(utcTimestampsMigrationService.migrate());
    }
}
