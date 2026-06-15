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

    private final net.friendly_bets.fourscore.FourScoreSyncService fourScoreSyncService;
    private final net.friendly_bets.twentyfourscore.TwentyFourScoreSyncService twentyFourScoreSyncService;
    private final AutoBetSettlementService autoBetSettlementService;

    @Scheduled(fixedDelayString = "${match-result-sync.polling-interval-ms}")
    public void pollExternalMatchdaysAndSettleBets() {
        try {
            int fourScoreSynced = fourScoreSyncService.runPollingTick();
            if (fourScoreSynced > 0) {
                log.debug("4score polled {} matchday(s)", fourScoreSynced);
            }
        } catch (Exception e) {
            log.warn("4score polling tick failed: {}", e.getMessage());
        }

        try {
            int twentyFourSynced = twentyFourScoreSyncService.runPollingTick();
            if (twentyFourSynced > 0) {
                log.debug("24score polled {} match(es)", twentyFourSynced);
            }
        } catch (Exception e) {
            log.warn("24score polling tick failed: {}", e.getMessage());
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
