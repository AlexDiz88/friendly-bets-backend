package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.DbBackupTelegramResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.DbBackupRecord;
import net.friendly_bets.repositories.DbBackupRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class DbBackupService {

    private static final Logger log = LoggerFactory.getLogger(DbBackupService.class);
    private static final int KEEP_NEWEST_COUNT = 3;

    private final MongoDatabaseDumpWriter dumpWriter;
    private final TelegramBackupClient telegramBackupClient;
    private final DbBackupRecordRepository backupRecordRepository;

    @Value("${app.backup.telegram.bot-token:}")
    private String botToken;

    @Value("${app.backup.telegram.chat-id:}")
    private String chatId;

    @Value("${app.backup.retention-days:365}")
    private int retentionDays;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public Snapshot createSnapshot() {
        if (!running.compareAndSet(false, true)) {
            throw new BadRequestException("dbBackupAlreadyRunning");
        }
        try {
            Instant snapshotUtc = DbBackupRetentionPolicy.truncateToSeconds(Instant.now());
            byte[] zipBytes = dumpWriter.writeZip();
            String filename = DbBackupRetentionPolicy.formatFilename(snapshotUtc);
            String sha256 = sha256Hex(zipBytes);
            return new Snapshot(snapshotUtc, filename, zipBytes, sha256);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("db backup dump failed: {}", e.getMessage(), e);
            throw new BadRequestException("dbBackupFailed");
        } finally {
            running.set(false);
        }
    }

    public DbBackupTelegramResultDto sendToTelegram(String trigger) {
        requireTelegramConfigured();
        Snapshot snapshot;
        try {
            snapshot = createSnapshot();
        } catch (BadRequestException e) {
            if (!"dbBackupAlreadyRunning".equals(e.getMessage())) {
                notifyFailure(null, e);
            }
            throw e;
        }
        boolean scheduled = DbBackupRecord.TRIGGER_SCHEDULED.equals(trigger);
        String caption = buildCaption(snapshot, trigger);
        try {
            int messageId = telegramBackupClient.sendDocument(
                    botToken.trim(),
                    chatId.trim(),
                    snapshot.zipBytes(),
                    snapshot.filename(),
                    caption,
                    scheduled
            );
            backupRecordRepository.save(DbBackupRecord.builder()
                    .createdAt(Instant.now())
                    .snapshotUtc(snapshot.snapshotUtc())
                    .filename(snapshot.filename())
                    .sizeBytes(snapshot.zipBytes().length)
                    .sha256(snapshot.sha256())
                    .trigger(trigger)
                    .telegramChatId(chatId.trim())
                    .telegramMessageId(messageId)
                    .build());
            log.info("db backup sent to telegram file={} bytes={} messageId={}",
                    snapshot.filename(), snapshot.zipBytes().length, messageId);
            return DbBackupTelegramResultDto.builder()
                    .filename(snapshot.filename())
                    .sizeBytes(snapshot.zipBytes().length)
                    .sha256(snapshot.sha256())
                    .snapshotUtc(snapshot.snapshotUtc().toString())
                    .telegramMessageId(messageId)
                    .message("dbBackupSentToTelegram")
                    .build();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("db backup telegram send failed: {}", e.getMessage(), e);
            notifyFailure(snapshot.snapshotUtc(), e);
            throw new BadRequestException("dbBackupTelegramSendFailed");
        }
    }

    private void notifyFailure(Instant snapshotUtc, Exception e) {
        String utc = snapshotUtc != null ? snapshotUtc.toString() : Instant.now().toString();
        telegramBackupClient.sendText(
                botToken.trim(),
                chatId.trim(),
                "FB_DB_BACKUP FAILED\nutc=" + utc + "\nerror=" + safeError(e)
        );
    }

    public void runScheduledBackup() {
        if (!isTelegramConfigured()) {
            log.debug("db backup skipped: telegram is not configured");
            return;
        }
        try {
            sendToTelegram(DbBackupRecord.TRIGGER_SCHEDULED);
        } catch (Exception e) {
            log.error("scheduled db backup failed: {}", e.getMessage(), e);
        }
        try {
            purgeExpiredTelegramBackups();
        } catch (Exception e) {
            log.error("db backup retention failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes only messages this app posted, and only when every age signal is older than retention.
     * Does not list or scan the Telegram channel.
     */
    public void purgeExpiredTelegramBackups() {
        if (!isTelegramConfigured()) {
            return;
        }
        int days = Math.max(retentionDays, 1);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        List<DbBackupRecord> active = backupRecordRepository.findByPurgedAtIsNullAndTelegramMessageIdNotNull();
        List<DbBackupRecord> toDelete = DbBackupRetentionPolicy.selectForPurge(
                active,
                cutoff,
                chatId.trim(),
                KEEP_NEWEST_COUNT
        );
        Instant now = Instant.now();
        for (DbBackupRecord record : toDelete) {
            try {
                boolean gone = telegramBackupClient.deleteMessage(
                        botToken.trim(),
                        record.getTelegramChatId(),
                        record.getTelegramMessageId()
                );
                if (gone) {
                    record.setPurgedAt(now);
                    backupRecordRepository.save(record);
                    log.info("db backup purged telegram messageId={} file={}",
                            record.getTelegramMessageId(), record.getFilename());
                }
            } catch (Exception e) {
                log.warn("db backup purge skipped messageId={} file={}: {}",
                        record.getTelegramMessageId(), record.getFilename(), e.getMessage());
            }
        }
    }

    public boolean isTelegramConfigured() {
        return botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }

    private void requireTelegramConfigured() {
        if (!isTelegramConfigured()) {
            throw new BadRequestException("telegramBackupNotConfigured");
        }
    }

    private static String buildCaption(Snapshot snapshot, String trigger) {
        return "FB_DB_BACKUP v1\n"
                + "utc=" + snapshot.snapshotUtc() + "\n"
                + "file=" + snapshot.filename() + "\n"
                + "bytes=" + snapshot.zipBytes().length + "\n"
                + "sha256=" + snapshot.sha256() + "\n"
                + "trigger=" + trigger;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String safeError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        String trimmed = message.replace('\n', ' ');
        return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
    }

    public record Snapshot(Instant snapshotUtc, String filename, byte[] zipBytes, String sha256) {
    }
}
