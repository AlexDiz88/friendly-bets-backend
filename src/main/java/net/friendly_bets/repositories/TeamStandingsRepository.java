package net.friendly_bets.repositories;

import net.friendly_bets.models.schedule.TeamStandings;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TeamStandingsRepository extends MongoRepository<TeamStandings, String> {

    Optional<TeamStandings> findBySeasonIdAndLeagueId(String seasonId, String leagueId);
}
