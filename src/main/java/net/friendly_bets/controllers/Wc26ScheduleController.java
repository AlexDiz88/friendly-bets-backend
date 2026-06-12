package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Wc26SchedulePageDto;
import net.friendly_bets.footballdata.config.FootballDataProperties;
import net.friendly_bets.wc26.Wc26ScheduleService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wc26")
public class Wc26ScheduleController {

    private final Wc26ScheduleService wc26ScheduleService;
    private final FootballDataProperties footballDataProperties;

    @GetMapping("/schedule")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Wc26SchedulePageDto> getSchedule(
            @RequestParam(required = false) String season
    ) {
        String resolvedSeason = season != null && !season.isBlank()
                ? season
                : footballDataProperties.getDefaultSeason();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(wc26ScheduleService.getSchedulePage(resolvedSeason));
    }
}
