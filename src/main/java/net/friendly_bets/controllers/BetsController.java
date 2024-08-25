package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.controllers.api.BetsApi;
import net.friendly_bets.dto.*;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.services.BetsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PostMapping("/add")
    public ResponseEntity<BetDto> addBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                         @RequestBody NewBet newOpenedBet) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(betsService.addOpenedBet(moderatorId, newOpenedBet));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PostMapping("/add/empty")
    public ResponseEntity<BetDto> addEmptyBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @RequestBody NewEmptyBet newEmptyBet) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(betsService.addEmptyBet(moderatorId, newEmptyBet));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PutMapping("{bet-id}/set-bet-result")
    public ResponseEntity<BetDto> setBetResult(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @PathVariable("bet-id") String betId,
                                               @RequestBody BetResult betResult) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity
                .ok(betsService.setBetResult(moderatorId, betId, betResult));
    }

    @Override
    @GetMapping("/opened/seasons/{season-id}")
    public ResponseEntity<BetsPage> getOpenedBets(@PathVariable("season-id") String seasonId) {
        return ResponseEntity
                .ok(betsService.getOpenedBets(seasonId));
    }

    @Override
    @GetMapping("/completed/seasons/{season-id}")
    public ResponseEntity<BetsPage> getCompletedBets(@PathVariable("season-id") String seasonId,
                                                     @RequestParam(required = false) String playerId,
                                                     @RequestParam(required = false) String leagueId,
                                                     @RequestParam(required = false, defaultValue = "0") int page,
                                                     @RequestParam(required = false, defaultValue = "14") int size,
                                                     @RequestParam(required = false) String sortBy) {
        Pageable pageable = PageRequest.of(page, size, sortBy != null ? Sort.by(sortBy) : Sort.by(Sort.Order.desc("betResultAddedAt")));
        return ResponseEntity
                .ok(betsService.getCompletedBets(seasonId, playerId, leagueId, pageable));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @GetMapping("/all/seasons/{season-id}")
    public ResponseEntity<BetsPage> getAllBets(@PathVariable("season-id") String seasonId,
                                               @RequestParam(required = false, defaultValue = "0") int page,
                                               @RequestParam(required = false, defaultValue = "10") int size,
                                               @RequestParam(required = false) String sortBy) {
        Pageable pageable = PageRequest.of(page, size, sortBy != null ? Sort.by(sortBy) : Sort.by(Sort.Order.desc("createdAt")));
        return ResponseEntity
                .ok(betsService.getAllBets(seasonId, pageable));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @PutMapping("/{bet-id}")
    public ResponseEntity<BetDto> editBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                          @PathVariable("bet-id") String betId,
                                          @RequestBody EditedBetDto editedBet) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity
                .ok(betsService.editBet(moderatorId, betId, editedBet));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    @DeleteMapping("/{bet-id}")
    public ResponseEntity<BetDto> deleteBet(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable("bet-id") String betId,
                                            @RequestBody DeletedBetDto deletedBetMetaData) {
        String moderatorId = currentUser.getUser().getId();
        return ResponseEntity.status(200)
                .body(betsService.deleteBet(moderatorId, betId, deletedBetMetaData));
    }
}
