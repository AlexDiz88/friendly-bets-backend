package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26BettingContextDto;
import net.friendly_bets.dto.Wc26GroupStageBoardDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import net.friendly_bets.wc26.Wc26MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/group-stage/board")
    public ResponseEntity<Wc26GroupStageBoardDto> groupStageBoard(@RequestParam String seasonId) {
        return ResponseEntity.ok(wc26MatchService.getGroupStageBoard(seasonId));
    }

}
