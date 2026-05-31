package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewTournamentFormatDto;
import net.friendly_bets.dto.TournamentFormatDto;
import net.friendly_bets.dto.TournamentFormatsPage;
import net.friendly_bets.dto.UpdateTournamentFormatDto;
import net.friendly_bets.services.TournamentFormatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tournament-formats")
public class TournamentFormatsController {

    private final TournamentFormatsService tournamentFormatsService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<TournamentFormatsPage> getAll() {
        return ResponseEntity.ok(tournamentFormatsService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<TournamentFormatDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(tournamentFormatsService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentFormatDto> create(@RequestBody @Valid NewTournamentFormatDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentFormatsService.create(dto));
    }

    @PatchMapping("/{id}")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TournamentFormatDto> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateTournamentFormatDto dto) {
        return ResponseEntity.ok(tournamentFormatsService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tournamentFormatsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
