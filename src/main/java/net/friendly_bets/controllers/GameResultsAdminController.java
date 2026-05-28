package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.AdminCorrectGameResultDto;
import net.friendly_bets.dto.ExternalMatchDto;
import net.friendly_bets.dto.MatchdaySettleResultDto;
import net.friendly_bets.dto.SettleMatchdayFromGameResultsDto;
import net.friendly_bets.gameresults.GameResultsAdminService;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/game-results")
public class GameResultsAdminController {

    private final GameResultsAdminService gameResultsAdminService;

    @PatchMapping("/{id}/admin-score")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<ExternalMatchDto> correctScore(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String id,
            @Valid @RequestBody AdminCorrectGameResultDto body
    ) {
        return ResponseEntity.ok(gameResultsAdminService.correctScoreByAdmin(id, body));
    }

    @PostMapping("/matchdays/settle-and-recalculate")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<MatchdaySettleResultDto> settleMatchdayAndRecalculate(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SettleMatchdayFromGameResultsDto body
    ) {
        return ResponseEntity.ok(
                gameResultsAdminService.settleMatchdayAndRecalculateStats(currentUser.getUser().getId(), body)
        );
    }
}
