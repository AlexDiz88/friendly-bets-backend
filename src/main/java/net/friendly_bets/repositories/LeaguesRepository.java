package net.friendly_bets.repositories;

import net.friendly_bets.models.League;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface LeaguesRepository extends MongoRepository<League, String> {

}
