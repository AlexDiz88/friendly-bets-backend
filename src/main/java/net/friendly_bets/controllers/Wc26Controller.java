package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26BettingContextDto;
import net.friendly_bets.dto.Wc26GameResultLookupDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.wc26.Wc26MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wc26")
public class Wc26Controller {

    private final Wc26MatchService wc26MatchService;

    @GetMapping("/betting-context")
    public ResponseEntity<Wc26BettingContextDto> bettingContext(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        String userId = currentUser != null && currentUser.getUser() != null
                ? currentUser.getUser().getId()
                : null;
        return ResponseEntity.ok(wc26MatchService.getBettingContext(userId));
    }

    @GetMapping("/matches/{wc26ScheduleId}/game-result")
    public ResponseEntity<Wc26GameResultLookupDto> gameResult(
            @PathVariable int wc26ScheduleId,
            @RequestParam String seasonId,
            @RequestParam(required = false) String slotId
    ) {
        return ResponseEntity.ok(wc26MatchService.lookupGameResult(seasonId, wc26ScheduleId, slotId));
    }
}
