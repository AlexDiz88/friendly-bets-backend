package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26FifaBracketPageDto;
import net.friendly_bets.dto.Wc26FifaStandingsPageDto;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.gameresults.config.MatchResultSyncProperties;
import net.friendly_bets.tournamentarchive.TournamentArchiveService;
import net.friendly_bets.tournamentarchive.TournamentArchiveViewService;
import net.friendly_bets.wc26.Wc26FifaLiveService;
import net.friendly_bets.wc26.Wc26ScheduleService;
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

    private final Wc26ScheduleService wc26ScheduleService;
    private final Wc26FifaLiveService wc26FifaLiveService;
    private final MatchResultSyncProperties matchResultSyncProperties;
    private final TournamentArchiveService tournamentArchiveService;
    private final TournamentArchiveViewService tournamentArchiveViewService;

    @GetMapping("/schedule")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26SchedulePageDto> getSchedule(
            @RequestParam(required = false) String season
    ) {
        if (tournamentArchiveService.exists(TournamentArchiveService.WC_2026)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                    .body(tournamentArchiveViewService.schedulePage(TournamentArchiveService.WC_2026));
        }
        String resolvedSeason = season != null && !season.isBlank()
                ? season
                : matchResultSyncProperties.getDefaultSeason();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(wc26ScheduleService.getSchedulePage(resolvedSeason));
    }

    @GetMapping("/fifa/standings")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26FifaStandingsPageDto> getFifaStandings(
            @RequestParam(required = false) String group
    ) {
        if (tournamentArchiveService.exists(TournamentArchiveService.WC_2026)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                    .body(tournamentArchiveViewService.standingsPage(TournamentArchiveService.WC_2026, group));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(wc26FifaLiveService.getStandings(group));
    }

    @GetMapping("/fifa/bracket")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26FifaBracketPageDto> getFifaBracket(
            @RequestParam(required = false) String stage
    ) {
        if (tournamentArchiveService.exists(TournamentArchiveService.WC_2026)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                    .body(tournamentArchiveViewService.bracketPage(TournamentArchiveService.WC_2026, stage));
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(wc26FifaLiveService.getBracket(stage));
    }
}
