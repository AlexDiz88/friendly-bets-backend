package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OddsApiSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(OddsApiSyncScheduler.class);

    private final OddsSyncCoordinator oddsSyncCoordinator;

    @Scheduled(fixedDelayString = "${odds-api.sync-interval-ms}")
    public void syncOdds() {
        try {
            oddsSyncCoordinator.runScheduledSync();
        } catch (Exception e) {
            log.warn("odds sync tick failed: {}", e.getMessage());
        }
    }
}
