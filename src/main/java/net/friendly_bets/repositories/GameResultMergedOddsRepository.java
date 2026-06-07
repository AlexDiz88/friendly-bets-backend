package net.friendly_bets.repositories;

import net.friendly_bets.models.odds.GameResultMergedOdds;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GameResultMergedOddsRepository extends MongoRepository<GameResultMergedOdds, String> {

    Optional<GameResultMergedOdds> findByGameResultId(String gameResultId);

    void deleteByGameResultId(String gameResultId);
}
