package net.friendly_bets.repositories;

import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GameResultsSyncRepository extends MongoRepository<GameResultsSync, String> {

    Optional<GameResultsSync> findByLeagueCodeAndMatchdayAndSeason(
            String leagueCode, int matchday, String season);

    List<GameResultsSync> findBySyncStatus(GameResultsSyncStatus syncStatus);
}
