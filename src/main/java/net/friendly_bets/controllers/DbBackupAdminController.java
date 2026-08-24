package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.DbBackupTelegramResultDto;
import net.friendly_bets.models.DbBackupRecord;
import net.friendly_bets.services.DbBackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/db-backup")
public class DbBackupAdminController {

    private final DbBackupService dbBackupService;

    @PostMapping("/telegram")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<DbBackupTelegramResultDto> sendToTelegram() {
        return ResponseEntity.ok(dbBackupService.sendToTelegram(DbBackupRecord.TRIGGER_MANUAL));
    }

    @GetMapping("/download")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> download() {
        DbBackupService.Snapshot snapshot = dbBackupService.createSnapshot();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + snapshot.filename() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(snapshot.zipBytes().length)
                .body(snapshot.zipBytes());
    }
}
