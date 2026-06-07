package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optional standalone Marathonbet tick (disabled by default — use {@link net.friendly_bets.oddsapi.OddsSyncCoordinator}).
 */
@Component
@ConditionalOnProperty(name = "marathonbet.standalone-scheduler", havingValue = "true")
@RequiredArgsConstructor
public class MarathonbetSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarathonbetSyncScheduler.class);

    private final MarathonbetSyncService marathonbetSyncService;

    @Scheduled(fixedDelayString = "${marathonbet.sync-interval-ms}")
    public void syncMarathonbet() {
        try {
            marathonbetSyncService.runTick();
        } catch (Exception e) {
            log.warn("marathonbet standalone sync tick failed: {}", e.getMessage());
        }
    }
}
