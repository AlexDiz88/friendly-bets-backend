package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbBackupScheduler {

    private final DbBackupService dbBackupService;

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}", zone = "${app.backup.zone:Europe/Berlin}")
    public void dailyBackup() {
        dbBackupService.runScheduledBackup();
    }
}
