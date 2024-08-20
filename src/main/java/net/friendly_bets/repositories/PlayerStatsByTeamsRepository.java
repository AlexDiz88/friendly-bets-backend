package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStatsByTeams;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface PlayerStatsByTeamsRepository extends MongoRepository<PlayerStatsByTeams, String> {

    Optional<PlayerStatsByTeams> findBySeasonIdAndLeagueIdAndUserId(String seasonId, String leagueId, String userId);

    void deleteBySeasonIdAndLeagueIdAndUserId(String seasonId, String leagueId, String userId);

    Optional<List<PlayerStatsByTeams>> findAllBySeasonId(String seasonId);

    void deleteAllBySeasonId(String seasonId);
}
