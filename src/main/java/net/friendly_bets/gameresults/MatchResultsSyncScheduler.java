package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchResultsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchResultsSyncScheduler.class);

    private final ExternalMatchResultPollingService externalMatchResultPollingService;

    @Scheduled(fixedDelayString = "${match-result-sync.polling-interval-ms}")
    public void pollExternalMatchdaysAndSettleBets() {
        try {
            externalMatchResultPollingService.runPollingTick();
        } catch (Exception e) {
            log.warn("Match-results polling tick failed: {}", e.getMessage());
        }
    }
}
