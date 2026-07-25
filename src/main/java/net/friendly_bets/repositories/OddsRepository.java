package net.friendly_bets.repositories;

import net.friendly_bets.models.odds.Odds;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OddsRepository extends MongoRepository<Odds, String> {

    Optional<Odds> findByMatchScheduleId(String matchScheduleId);

    void deleteByMatchScheduleId(String matchScheduleId);
}
