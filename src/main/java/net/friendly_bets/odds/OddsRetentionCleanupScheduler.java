package net.friendly_bets.odds;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OddsRetentionCleanupScheduler {

    private final OddsRetentionCleanupService oddsRetentionCleanupService;

    @Scheduled(
            cron = "${odds.retention-cleanup.cron:0 30 2 * * *}",
            zone = "${odds.retention-cleanup.zone:Europe/Berlin}"
    )
    public void dailyPurge() {
        oddsRetentionCleanupService.purgeExpired();
    }
}
