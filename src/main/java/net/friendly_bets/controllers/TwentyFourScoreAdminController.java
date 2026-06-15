package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.twentyfourscore.TwentyFourScorePreviewMatchDto;
import net.friendly_bets.twentyfourscore.TwentyFourScoreSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/twentyfourscore")
public class TwentyFourScoreAdminController {

    private final TwentyFourScoreSyncService twentyFourScoreSyncService;

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<TwentyFourScorePreviewMatchDto>> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(twentyFourScoreSyncService.previewDate(date));
    }
}
