package net.friendly_bets.repositories;

import net.friendly_bets.models.gameresults.MatchResultSyncSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchResultSyncSettingsRepository extends MongoRepository<MatchResultSyncSettings, String> {
}
