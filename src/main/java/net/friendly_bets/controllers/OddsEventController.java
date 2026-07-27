package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsEventMarketsDto;
import net.friendly_bets.odds.OddsPresentationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/odds/events")
public class OddsEventController {

    private final OddsPresentationService oddsPresentationService;

    @GetMapping("/{matchScheduleId}")
    @PreAuthorize("hasAuthority('USER') || hasAuthority('MODERATOR') || hasAuthority('ADMIN')")
    public ResponseEntity<OddsEventMarketsDto> getEventMarkets(@PathVariable String matchScheduleId) {
        return ResponseEntity.ok(oddsPresentationService.getMarketsForMatchSchedule(matchScheduleId));
    }
}
