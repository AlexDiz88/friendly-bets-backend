package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface PlayerStatsRepository extends MongoRepository<PlayerStats, String> {

    List<PlayerStats> findAllBySeasonId(String seasonId);

    Optional<PlayerStats> findAllBySeasonIdAndUser(String seasonId, User user);

    Optional<PlayerStats> findBySeasonIdAndLeagueIdAndUser(String seasonId, String leagueId, User user);

    void deleteAllBySeasonId(String seasonId);
}
