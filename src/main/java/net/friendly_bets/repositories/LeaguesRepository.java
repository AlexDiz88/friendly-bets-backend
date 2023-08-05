package net.friendly_bets.repositories;

import net.friendly_bets.models.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface LeaguesRepository extends JpaRepository<League, Long> {

}
