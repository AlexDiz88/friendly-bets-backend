package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsDemoEventDetailDto;
import net.friendly_bets.dto.OddsDemoEventSummaryDto;
import net.friendly_bets.dto.OddsDemoRefreshResultDto;
import net.friendly_bets.oddsapi.OddsDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/odds/demo")
public class OddsDemoController {

    private final OddsDemoService oddsDemoService;

    @GetMapping("/leagues/{leagueSlug}/events")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<OddsDemoEventSummaryDto>> listEvents(@PathVariable String leagueSlug) {
        return ResponseEntity.ok(oddsDemoService.listByLeague(leagueSlug));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OddsDemoEventDetailDto> getEvent(@PathVariable long eventId) {
        return ResponseEntity.ok(oddsDemoService.getByEventId(eventId));
    }

    @PostMapping("/leagues/{leagueSlug}/refresh")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoRefreshResultDto> refresh(
            @PathVariable String leagueSlug,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(oddsDemoService.refreshLeagueDemo(leagueSlug, limit));
    }
}
