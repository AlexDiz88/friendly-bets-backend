package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.oddsapi.OddsSyncCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Staggered Marathonbet ticks: current slot every 6h, next slot every 6h offset by 3h.
 */
@Component
@ConditionalOnProperty(name = "marathonbet.standalone-scheduler", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class MarathonbetOddsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarathonbetOddsSyncScheduler.class);

    private final OddsSyncCoordinator oddsSyncCoordinator;

    @Scheduled(fixedDelayString = "${marathonbet.slot-sync-interval-ms}")
    public void syncCurrentSlot() {
        try {
            oddsSyncCoordinator.runMarathonSlotSync(MarathonbetSlotScope.CURRENT);
        } catch (Exception e) {
            log.warn("marathonbet current-slot sync failed: {}", e.getMessage());
        }
    }

    @Scheduled(
            fixedDelayString = "${marathonbet.slot-sync-interval-ms}",
            initialDelayString = "${marathonbet.slot-sync-stagger-ms}"
    )
    public void syncNextSlot() {
        try {
            oddsSyncCoordinator.runMarathonSlotSync(MarathonbetSlotScope.NEXT);
        } catch (Exception e) {
            log.warn("marathonbet next-slot sync failed: {}", e.getMessage());
        }
    }
}
