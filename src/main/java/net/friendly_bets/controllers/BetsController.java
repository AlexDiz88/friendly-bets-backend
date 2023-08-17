package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.BetsApi;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.dto.SeasonDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.BetsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bets")
public class BetsController implements BetsApi {

    private final BetsService betsService;

    @Override
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<BetsPage> getAllBets(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity
                .ok(betsService.getAllBets());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PutMapping("/{bet-id}")
    public ResponseEntity<BetDto> editBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                             @PathVariable("bet-id") String betId,
                                             @RequestBody EditedBetDto editedBet) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity.status(201)
                .body(betsService.editBet(moderatorId, betId, editedBet));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{bet-id}")
    public ResponseEntity<BetDto> deleteBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable("bet-id") String betId) {
        return null;
    }

}
