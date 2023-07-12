package net.friendly_bets.repositories;

import net.friendly_bets.models.Season;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface SeasonsRepository extends MongoRepository<Season, String> {

}
