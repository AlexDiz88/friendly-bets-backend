package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStatsByBetValues;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStatsByBetValuesRepository extends MongoRepository<PlayerStatsByBetValues, String> {

    Optional<PlayerStatsByBetValues> findBySeasonIdAndLeagueIdAndUserId(String seasonId, String leagueId, String userId);

    List<PlayerStatsByBetValues> findAllBySeasonId(String seasonId);

    void deleteAllBySeasonId(String seasonId);
}
