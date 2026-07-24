package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.tournamentarchive.TournamentArchive;
import net.friendly_bets.tournamentarchive.TournamentArchiveService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tournament-archives")
public class TournamentArchiveController {

    private final TournamentArchiveService tournamentArchiveService;

    @GetMapping("/{editionCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TournamentArchive> getArchive(@PathVariable String editionCode) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(tournamentArchiveService.getByEditionCode(editionCode));
    }
}
