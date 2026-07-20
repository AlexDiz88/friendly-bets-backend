package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.tournamentarchive.TournamentArchiveFifaExportService;
import net.friendly_bets.tournamentarchive.TournamentArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tournament-archives")
public class TournamentArchiveAdminController {

    private final TournamentArchiveFifaExportService exportService;
    private final TournamentArchiveService tournamentArchiveService;

    /**
     * Тянет FIFA → пишет review JSON на диск и возвращает тот же документ в body.
     */
    @PostMapping("/export-fifa")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentArchive> exportFifa(
            @RequestParam(required = false, defaultValue = "WC_2026") String editionCode
    ) {
        return ResponseEntity.ok(exportService.exportAndWriteFile(editionCode));
    }

    /** Импорт из тела запроса (после ручной правки JSON). */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentArchive> importBody(@RequestBody TournamentArchive archive) {
        return ResponseEntity.ok(tournamentArchiveService.importArchive(archive));
    }

    /** Импорт из файла data/tournament-archive-*.json на сервере. */
    @PostMapping("/import-file")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentArchive> importFile(
            @RequestParam(required = false, defaultValue = "WC_2026") String editionCode
    ) {
        return ResponseEntity.ok(tournamentArchiveService.importFromReviewFile(editionCode));
    }
}
