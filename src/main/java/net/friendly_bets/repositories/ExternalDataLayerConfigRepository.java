package net.friendly_bets.repositories;

import net.friendly_bets.models.providers.ExternalDataLayerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExternalDataLayerConfigRepository extends MongoRepository<ExternalDataLayerConfig, String> {
}
