package net.friendly_bets.repositories;

import net.friendly_bets.models.AuthRateLimit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthRateLimitRepository extends MongoRepository<AuthRateLimit, String> {
}
