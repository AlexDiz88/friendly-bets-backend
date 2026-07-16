package net.friendly_bets.repositories;

import net.friendly_bets.models.ClientVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientVersionRepository extends MongoRepository<ClientVersion, String> {
}
