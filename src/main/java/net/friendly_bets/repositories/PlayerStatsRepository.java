package net.friendly_bets.repositories;

import net.friendly_bets.models.League;
import net.friendly_bets.models.PlayerStats;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface PlayerStatsRepository extends MongoRepository<PlayerStats, String> {

    Optional<PlayerStats> findBySeasonIdAndLeagueIdAndUserId(String seasonId, String leagueId, String userId);
}
