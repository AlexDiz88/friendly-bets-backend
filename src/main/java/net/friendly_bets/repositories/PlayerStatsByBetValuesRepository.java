package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStatsByBetValues;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlayerStatsByBetValuesRepository extends MongoRepository<PlayerStatsByBetValues, String> {

    List<PlayerStatsByBetValues> findAllBySeasonIdAndLeagueIdAndUserId(String seasonId, String leagueId, String userId);

    List<PlayerStatsByBetValues> findAllBySeasonId(String seasonId);

    void deleteAllBySeasonId(String seasonId);
}
