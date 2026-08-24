package net.friendly_bets.repositories;

import net.friendly_bets.models.DbBackupRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DbBackupRecordRepository extends MongoRepository<DbBackupRecord, String> {

    List<DbBackupRecord> findByPurgedAtIsNullAndTelegramMessageIdNotNull();
}
