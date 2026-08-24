package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "db_backup_records")
public class DbBackupRecord {

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    @MongoId
    @Field(name = "_id")
    private String id;

    /** When this process created/sent the snapshot (UTC). */
    @Field(name = "created_at")
    @Indexed
    private Instant createdAt;

    /** Snapshot instant truncated to seconds — must match filename. */
    @Field(name = "snapshot_utc")
    private Instant snapshotUtc;

    @Field(name = "filename")
    private String filename;

    @Field(name = "size_bytes")
    private long sizeBytes;

    @Field(name = "sha256")
    private String sha256;

    @Field(name = "trigger")
    private String trigger;

    @Field(name = "telegram_chat_id")
    private String telegramChatId;

    @Field(name = "telegram_message_id")
    private Integer telegramMessageId;

    /** Set only after a successful Telegram delete (or message already gone). */
    @Field(name = "purged_at")
    private Instant purgedAt;
}
