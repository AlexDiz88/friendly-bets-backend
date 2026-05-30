package net.friendly_bets.repositories;

import net.friendly_bets.models.BetPlacementIdempotency;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BetPlacementIdempotencyRepository extends MongoRepository<BetPlacementIdempotency, String> {

    Optional<BetPlacementIdempotency> findByIdempotencyKey(String idempotencyKey);
}
