package net.friendly_bets.services;

import net.friendly_bets.models.DbBackupRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbBackupRetentionPolicyTest {

    private static final String CHAT = "-1001234567890";

    @Test
    void filenameRoundTrip_usesUtcSeconds() {
        Instant snapshot = Instant.parse("2026-08-24T01:00:00Z");
        String filename = DbBackupRetentionPolicy.formatFilename(snapshot);
        assertEquals("FriendlyBets-backup-20260824-010000Z.zip", filename);
        Optional<Instant> parsed = DbBackupRetentionPolicy.parseSnapshotUtcFromFilename(filename);
        assertEquals(Optional.of(snapshot), parsed);
    }

    @Test
    void parse_rejectsForeignFilenames() {
        assertTrue(DbBackupRetentionPolicy.parseSnapshotUtcFromFilename("random.zip").isEmpty());
        assertTrue(DbBackupRetentionPolicy.parseSnapshotUtcFromFilename("FriendlyBets-backup-20260824.zip").isEmpty());
        assertTrue(DbBackupRetentionPolicy.parseSnapshotUtcFromFilename(null).isEmpty());
    }

    @Test
    void selectForPurge_neverDeletesNewestThree_evenIfOlderThanCutoff() {
        Instant now = Instant.parse("2026-08-24T03:00:00Z");
        Instant cutoff = now.minus(365, ChronoUnit.DAYS);
        List<DbBackupRecord> records = List.of(
                record("1", now.minus(400, ChronoUnit.DAYS), 11),
                record("2", now.minus(399, ChronoUnit.DAYS), 12),
                record("3", now.minus(398, ChronoUnit.DAYS), 13)
        );

        List<DbBackupRecord> purge = DbBackupRetentionPolicy.selectForPurge(records, cutoff, CHAT, 3);

        assertTrue(purge.isEmpty());
    }

    @Test
    void selectForPurge_deletesOnlyOlderThanCutoff_notRecent() {
        Instant now = Instant.parse("2026-08-24T03:00:00Z");
        Instant cutoff = now.minus(365, ChronoUnit.DAYS);
        DbBackupRecord newest = record("new", now.minus(1, ChronoUnit.DAYS), 40);
        DbBackupRecord mid = record("mid", now.minus(10, ChronoUnit.DAYS), 30);
        DbBackupRecord oldEnough = record("old", now.minus(366, ChronoUnit.DAYS), 20);
        DbBackupRecord older = record("older", now.minus(400, ChronoUnit.DAYS), 10);

        List<DbBackupRecord> purge = DbBackupRetentionPolicy.selectForPurge(
                List.of(newest, mid, oldEnough, older),
                cutoff,
                CHAT,
                3
        );

        assertEquals(1, purge.size());
        assertEquals("older", purge.get(0).getId());
    }

    @Test
    void selectForPurge_skipsWrongChatId() {
        Instant now = Instant.parse("2026-08-24T03:00:00Z");
        Instant cutoff = now.minus(365, ChronoUnit.DAYS);
        DbBackupRecord foreign = record("x", now.minus(400, ChronoUnit.DAYS), 5);
        foreign.setTelegramChatId("-100999");
        List<DbBackupRecord> keepNewest = List.of(
                record("a", now.minus(1, ChronoUnit.DAYS), 8),
                record("b", now.minus(2, ChronoUnit.DAYS), 7),
                record("c", now.minus(3, ChronoUnit.DAYS), 6),
                foreign
        );

        List<DbBackupRecord> purge = DbBackupRetentionPolicy.selectForPurge(keepNewest, cutoff, CHAT, 3);

        assertTrue(purge.isEmpty());
    }

    @Test
    void selectForPurge_skipsFilenameMismatch() {
        Instant now = Instant.parse("2026-08-24T03:00:00Z");
        Instant cutoff = now.minus(365, ChronoUnit.DAYS);
        DbBackupRecord tampered = record("t", now.minus(400, ChronoUnit.DAYS), 4);
        tampered.setFilename("FriendlyBets-backup-20260820-000000Z.zip");
        List<DbBackupRecord> records = List.of(
                record("a", now.minus(1, ChronoUnit.DAYS), 8),
                record("b", now.minus(2, ChronoUnit.DAYS), 7),
                record("c", now.minus(3, ChronoUnit.DAYS), 6),
                tampered
        );

        List<DbBackupRecord> purge = DbBackupRetentionPolicy.selectForPurge(records, cutoff, CHAT, 3);

        assertTrue(purge.isEmpty());
    }

    @Test
    void isSafeToDelete_falseWhenExactlyAtCutoff() {
        Instant cutoff = Instant.parse("2025-08-24T03:00:00Z");
        DbBackupRecord atCutoff = record("eq", cutoff, 1);
        assertFalse(DbBackupRetentionPolicy.isSafeToDelete(atCutoff, cutoff, CHAT));
    }

    private static DbBackupRecord record(String id, Instant snapshot, int messageId) {
        Instant truncated = DbBackupRetentionPolicy.truncateToSeconds(snapshot);
        return DbBackupRecord.builder()
                .id(id)
                .createdAt(truncated)
                .snapshotUtc(truncated)
                .filename(DbBackupRetentionPolicy.formatFilename(truncated))
                .telegramChatId(CHAT)
                .telegramMessageId(messageId)
                .build();
    }
}
