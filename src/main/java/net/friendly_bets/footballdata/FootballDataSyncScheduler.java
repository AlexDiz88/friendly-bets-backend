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
    private final AutoBetSettlementService autoBetSettlementService;

    /** Каждые 15 мин: опрос API → auto-settle открытых ставок. */
    @Scheduled(fixedDelayString = "${football-data.polling-interval-ms}")
    public void pollExternalMatchdaysAndSettleBets() {
        try {
            footballDataSyncService.runPollingTick();
        } catch (Exception e) {
            log.warn("Football-data polling tick failed: {}", e.getMessage());
        }

        try {
            autoBetSettlementService.settleActiveSeasonIfEnabled()
                    .ifPresent(result -> {
                        if (result.isExecuted()) {
                            log.info(
                                    "Auto-settle tick: season={}, matches={}, bets={}",
                                    result.getSeasonId(),
                                    result.getMatchesSubmitted(),
                                    result.getBetsProcessed()
                            );
                        }
                    });
        } catch (Exception e) {
            log.error("Auto-settle tick failed: {}", e.getMessage(), e);
        }
    }
}
