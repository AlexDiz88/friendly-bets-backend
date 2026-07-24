package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26BracketPageDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.dto.Wc26StandingsPageDto;
import net.friendly_bets.tournamentarchive.TournamentArchiveService;
import net.friendly_bets.tournamentarchive.TournamentArchiveViewService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wc26")
public class Wc26ScheduleController {

    private final TournamentArchiveViewService tournamentArchiveViewService;

    @GetMapping("/schedule")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26SchedulePageDto> getSchedule() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(tournamentArchiveViewService.schedulePage(TournamentArchiveService.WC_2026));
    }

    @GetMapping("/standings")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26StandingsPageDto> getStandings(
            @RequestParam(required = false) String group
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(tournamentArchiveViewService.standingsPage(TournamentArchiveService.WC_2026, group));
    }

    @GetMapping("/bracket")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26BracketPageDto> getBracket(
            @RequestParam(required = false) String stage
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(tournamentArchiveViewService.bracketPage(TournamentArchiveService.WC_2026, stage));
    }
}
