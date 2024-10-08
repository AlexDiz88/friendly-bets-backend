package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.CalendarsApi;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.CalendarNodeDto;
import net.friendly_bets.dto.CalendarNodesPage;
import net.friendly_bets.dto.NewCalendarNodeDto;
import net.friendly_bets.services.CalendarsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendars")
public class CalendarsController implements CalendarsApi {

    private final CalendarsService calendarsService;

    @Override
    @GetMapping("/seasons/{season-id}")
    public ResponseEntity<CalendarNodesPage> getAllSeasonCalendarNodes(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.ok(calendarsService.getAllSeasonCalendarNodes(seasonId));
    }

    @Override
    @GetMapping("/seasons/{season-id}/has-bets")
    public ResponseEntity<CalendarNodesPage> getSeasonCalendarHasBetsNodes(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.ok(calendarsService.getSeasonCalendarHasBetsNodes(seasonId));
    }

    @Override
    @GetMapping("/seasons/{season-id}/actual")
    public ResponseEntity<BetsPage> getActualCalendarNodeBets(@PathVariable("season-id") String seasonId) {
        return ResponseEntity.ok(calendarsService.getActualCalendarNodeBets(seasonId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PostMapping
    public ResponseEntity<CalendarNodeDto> createCalendarNode(@RequestBody @Valid NewCalendarNodeDto newCalendarNode) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(calendarsService.createCalendarNode(newCalendarNode));
    }

    @Override
    @GetMapping("/{calendar-id}/bets")
    public ResponseEntity<BetsPage> getBetsByCalendarNode(@PathVariable("calendar-id") String calendarNodeId) {
        return ResponseEntity.ok(calendarsService.getBetsByCalendarNode(calendarNodeId));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @DeleteMapping("/{calendar-id}")
    public ResponseEntity<CalendarNodeDto> deleteCalendarNode(@PathVariable("calendar-id") String calendarNodeId) {
        return ResponseEntity.ok(calendarsService.deleteCalendarNode(calendarNodeId));
    }
}
