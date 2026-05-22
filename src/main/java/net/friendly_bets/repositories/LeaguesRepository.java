package net.friendly_bets.repositories;

import net.friendly_bets.models.League;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface LeaguesRepository extends MongoRepository<League, String> {

    List<League> findAllByLeagueCode(League.LeagueCode leagueCode);
}
