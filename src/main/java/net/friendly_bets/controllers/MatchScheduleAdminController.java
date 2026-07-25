package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MatchdaySettleResultDto;
import net.friendly_bets.dto.SettleMatchdayFromGameResultsDto;
import net.friendly_bets.services.MatchScheduleSettleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/match-schedules")
public class MatchScheduleAdminController {

    private final MatchScheduleSettleService matchScheduleSettleService;

    @PostMapping("/matchdays/settle-and-recalculate")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<MatchdaySettleResultDto> settleMatchday(
            @AuthenticationPrincipal(expression = "id") String currentUserId,
            @Valid @RequestBody SettleMatchdayFromGameResultsDto body
    ) {
        return ResponseEntity.ok(
                matchScheduleSettleService.settleMatchdayAndRecalculateStats(currentUserId, body)
        );
    }
}
