package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.*;
import net.friendly_bets.oddsapi.OddsDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

    @GetMapping("/leagues/{leagueSlug}/api-events")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<List<OddsDemoEventIdDto>> listApiEvents(
            @PathVariable String leagueSlug,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(oddsDemoService.listEventIdsFromApi(leagueSlug, limit));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OddsDemoEventDetailDto> getEvent(@PathVariable long eventId) {
        return ResponseEntity.ok(oddsDemoService.getByEventId(eventId));
    }

    @GetMapping("/events/{eventId}/debug")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoDebugDto> getEventDebug(@PathVariable long eventId) {
        return ResponseEntity.ok(oddsDemoService.getDebugByEventId(eventId));
    }

    @PostMapping("/leagues/{leagueSlug}/refresh")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoRefreshResultDto> refresh(
            @PathVariable String leagueSlug,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(oddsDemoService.refreshLeagueDemo(leagueSlug, limit));
    }

    @PostMapping("/events/refresh")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoRefreshResultDto> refreshEvents(
            @Valid @RequestBody OddsDemoRefreshEventsRequestDto request
    ) {
        return ResponseEntity.ok(oddsDemoService.refreshEventsByIds(request.getLeagueSlug(), request.getEventIds()));
    }

    @DeleteMapping("/events/{eventId}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoClearResultDto> deleteEvent(@PathVariable long eventId) {
        oddsDemoService.deleteEvent(eventId);
        return ResponseEntity.ok(OddsDemoClearResultDto.builder().deletedCount(1).build());
    }

    @DeleteMapping("/leagues/{leagueSlug}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoClearResultDto> clearLeague(@PathVariable String leagueSlug) {
        return ResponseEntity.ok(oddsDemoService.clearLeague(leagueSlug));
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<OddsDemoClearResultDto> clearAll() {
        return ResponseEntity.ok(oddsDemoService.clearAll());
    }
}
