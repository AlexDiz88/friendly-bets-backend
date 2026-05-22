package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FootballDataSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FootballDataSyncScheduler.class);

    private final FootballDataSyncService footballDataSyncService;

    /** Каждые 15 мин — текущий тур по каждому турниру, если он не завершён. */
    @Scheduled(fixedDelayString = "${football-data.polling-interval-ms}")
    public void pollExternalMatchdays() {
        int synced = footballDataSyncService.syncPollingMatchdays();
        if (synced > 0) {
            log.debug("Football-data polled {} current matchday(s)", synced);
        }
    }
}
