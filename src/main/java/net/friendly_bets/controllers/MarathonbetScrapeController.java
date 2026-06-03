package net.friendly_bets.controllers;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetScrapeResultDto;
import net.friendly_bets.marathonbet.MarathonbetScrapeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marathonbet/scrape")
public class MarathonbetScrapeController {

    private final MarathonbetScrapeService marathonbetScrapeService;

    @GetMapping("/events/{treeId}")
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MODERATOR')")
    public ResponseEntity<MarathonbetScrapeResultDto> scrapeEvent(@PathVariable long treeId) {
        return ResponseEntity.ok(marathonbetScrapeService.scrapeByTreeId(treeId));
    }
}
