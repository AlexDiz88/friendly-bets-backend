package net.friendly_bets.repositories;

import net.friendly_bets.models.Bet;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface BetsRepository extends MongoRepository<Bet, String> {

}
