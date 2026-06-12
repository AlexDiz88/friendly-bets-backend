package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FourScoreSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FourScoreSyncScheduler.class);

    private final FourScoreSyncService fourScoreSyncService;

    /** Каждые 5 мин: live и ещё не финализированные матчи с 4score (без неначавшихся/завершённых в БД). */
    @Scheduled(fixedDelayString = "${fourscore.polling-interval-ms}")
    public void pollLiveMatchesFromFourScore() {
        try {
            int updated = fourScoreSyncService.runPollingTick();
            if (updated > 0) {
                log.debug("4score live poll updated {} match(es)", updated);
            }
        } catch (Exception e) {
            log.warn("4score live polling tick failed: {}", e.getMessage());
        }
    }
}
