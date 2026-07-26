package net.friendly_bets.repositories;

import net.friendly_bets.models.AppSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppSettingsRepository extends MongoRepository<AppSettings, String> {
}
