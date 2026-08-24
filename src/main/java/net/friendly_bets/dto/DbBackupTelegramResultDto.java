package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DbBackupTelegramResultDto {
    String filename;
    long sizeBytes;
    String sha256;
    String snapshotUtc;
    Integer telegramMessageId;
    String message;
}
