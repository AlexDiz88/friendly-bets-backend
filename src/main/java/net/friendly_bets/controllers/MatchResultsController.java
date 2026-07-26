package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalMatchdayPageDto;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match-results")
public class MatchResultsController {

    private final MatchScheduleQueryService matchScheduleQueryService;
    private final MatchScheduleDisplayService matchScheduleDisplayService;

    @GetMapping("/competitions/{pathLeagueCode}/matchdays/{matchday}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ExternalMatchdayPageDto> getMatchday(
            @PathVariable String pathLeagueCode,
            @PathVariable int matchday,
            @RequestParam(defaultValue = "2025") String season,
            @RequestParam(required = false) String leagueId) {

        List<MatchSchedule> matches = matchScheduleQueryService.getMatches(
                pathLeagueCode, matchday, season, leagueId);

        return ResponseEntity.ok(ExternalMatchdayPageDto.builder()
                .matches(matchScheduleDisplayService.toDisplayDtos(matches, season))
                .build());
    }
}
