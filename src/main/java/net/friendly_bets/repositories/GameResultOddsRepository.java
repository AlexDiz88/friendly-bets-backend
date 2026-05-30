package net.friendly_bets.repositories;

import net.friendly_bets.models.odds.GameResultOdds;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GameResultOddsRepository extends MongoRepository<GameResultOdds, String> {

    Optional<GameResultOdds> findByGameResultIdAndBookmaker(String gameResultId, String bookmaker);

    List<GameResultOdds> findByGameResultId(String gameResultId);
}
