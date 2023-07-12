package net.friendly_bets.repositories;

import net.friendly_bets.models.Team;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface TeamsRepository extends MongoRepository<Team, String> {

}
