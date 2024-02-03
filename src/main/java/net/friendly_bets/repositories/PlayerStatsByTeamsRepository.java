package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStatsByTeams;
import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface PlayerStatsByTeamsRepository extends MongoRepository<PlayerStatsByTeams, String> {

    Optional<PlayerStatsByTeams> findBySeasonIdAndLeagueIdAndUserAndIsLeagueStats(String seasonId, String leagueId, User user, boolean isLeagueStats);

    Optional<PlayerStatsByTeams> findBySeasonIdAndLeagueIdAndIsLeagueStats(String seasonId, String leagueId, boolean isLeagueStats);

    Optional<List<PlayerStatsByTeams>> findAllBySeasonId(String seasonId);

    void deleteAllBySeasonId(String seasonId);


}
