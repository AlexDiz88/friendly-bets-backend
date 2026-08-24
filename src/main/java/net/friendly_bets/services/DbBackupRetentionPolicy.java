package net.friendly_bets.services;

import net.friendly_bets.models.DbBackupRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure rules for which Telegram backup messages may be deleted.
 * Never scans the channel — only records this app stored after a successful send.
 */
public final class DbBackupRetentionPolicy {

    public static final String FILENAME_PREFIX = "FriendlyBets-backup-";
    public static final Pattern FILENAME_PATTERN =
            Pattern.compile("^FriendlyBets-backup-(\\d{8})-(\\d{6})Z\\.zip$");
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private DbBackupRetentionPolicy() {
    }

    public static Instant truncateToSeconds(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS);
    }

    public static String formatFilename(Instant snapshotUtc) {
        Instant truncated = truncateToSeconds(snapshotUtc);
        DateTimeFormatter named = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
        return FILENAME_PREFIX + named.format(truncated) + "Z.zip";
    }

    /**
     * @return parsed UTC instant, or empty if the name is not our backup filename
     */
    public static java.util.Optional<Instant> parseSnapshotUtcFromFilename(String filename) {
        if (filename == null) {
            return java.util.Optional.empty();
        }
        Matcher matcher = FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return java.util.Optional.empty();
        }
        try {
            LocalDateTime local = LocalDateTime.parse(matcher.group(1) + matcher.group(2), FILE_TS);
            return java.util.Optional.of(local.toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Records that are safe to delete in Telegram.
     * <ul>
     *   <li>never the {@code keepNewestCount} newest by {@code createdAt}</li>
     *   <li>never if filename / stored snapshot / createdAt disagree</li>
     *   <li>never if chat id does not match the currently configured channel</li>
     *   <li>only if createdAt, snapshotUtc and filename date are all strictly before cutoff</li>
     * </ul>
     */
    public static List<DbBackupRecord> selectForPurge(
            List<DbBackupRecord> activeTelegramBackups,
            Instant cutoff,
            String expectedChatId,
            int keepNewestCount
    ) {
        if (activeTelegramBackups == null || activeTelegramBackups.isEmpty() || cutoff == null) {
            return List.of();
        }
        if (expectedChatId == null || expectedChatId.isBlank()) {
            return List.of();
        }
        int protect = Math.max(keepNewestCount, 1);

        List<DbBackupRecord> newestFirst = new ArrayList<>(activeTelegramBackups);
        newestFirst.sort(Comparator
                .comparing(DbBackupRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DbBackupRecord::getSnapshotUtc, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());

        List<DbBackupRecord> eligible = new ArrayList<>();
        int index = 0;
        for (DbBackupRecord record : newestFirst) {
            index++;
            if (index <= protect) {
                continue;
            }
            if (isSafeToDelete(record, cutoff, expectedChatId)) {
                eligible.add(record);
            }
        }
        return eligible;
    }

    static boolean isSafeToDelete(DbBackupRecord record, Instant cutoff, String expectedChatId) {
        if (record == null || record.getPurgedAt() != null) {
            return false;
        }
        if (record.getTelegramMessageId() == null || record.getTelegramMessageId() <= 0) {
            return false;
        }
        if (record.getTelegramChatId() == null || !expectedChatId.equals(record.getTelegramChatId())) {
            return false;
        }
        Instant createdAt = record.getCreatedAt();
        Instant storedSnapshot = record.getSnapshotUtc();
        if (createdAt == null || storedSnapshot == null) {
            return false;
        }
        java.util.Optional<Instant> filenameUtc = parseSnapshotUtcFromFilename(record.getFilename());
        if (filenameUtc.isEmpty()) {
            return false;
        }
        Instant fromFile = filenameUtc.get();
        if (!truncateToSeconds(storedSnapshot).equals(fromFile)) {
            return false;
        }
        return createdAt.isBefore(cutoff)
                && storedSnapshot.isBefore(cutoff)
                && fromFile.isBefore(cutoff);
    }
}
