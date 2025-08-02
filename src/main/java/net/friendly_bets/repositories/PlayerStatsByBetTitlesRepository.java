package net.friendly_bets.repositories;

import net.friendly_bets.models.PlayerStatsByBetTitles;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface PlayerStatsByBetTitlesRepository extends MongoRepository<PlayerStatsByBetTitles, String> {

    Optional<PlayerStatsByBetTitles> findBySeasonIdAndUserId(String seasonId, String userId);

    Optional<List<PlayerStatsByBetTitles>> findAllBySeasonId(String seasonId);

    void deleteAllBySeasonId(String seasonId);
}
