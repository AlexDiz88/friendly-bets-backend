package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreSyncScheduler.class);

    private final TwentyFourScoreSyncService twentyFourScoreSyncService;

    @Scheduled(fixedDelayString = "${twentyfourscore.polling-interval-ms}")
    public void pollSecondaryScoresFromTwentyFourScore() {
        try {
            int updated = twentyFourScoreSyncService.runPollingTick();
            if (updated > 0) {
                log.debug("24score secondary poll updated {} match(es)", updated);
            }
        } catch (Exception e) {
            log.warn("24score polling tick failed: {}", e.getMessage());
        }
    }
}
