package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStats;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface PlayerStatsRepository extends MongoRepository<PlayerStats, String> {

    List<PlayerStats> findAllBySeasonId(String seasonId);

    List<PlayerStats> findAllBySeasonIdAndLeagueIdAndUser_Id(String seasonId, String leagueId, String userId);

    void deleteAllBySeasonId(String seasonId);
}
