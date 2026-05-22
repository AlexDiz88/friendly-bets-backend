package net.friendly_bets.repositories;

import net.friendly_bets.models.Team;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface TeamsRepository extends MongoRepository<Team, String> {

    boolean existsByTitle(String title);

    Optional<Team> findByFootballDataTeamId(int footballDataTeamId);

    Optional<Team> findByTitleIgnoreCase(String title);
}
