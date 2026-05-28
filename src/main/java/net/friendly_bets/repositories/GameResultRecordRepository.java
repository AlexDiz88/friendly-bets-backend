package net.friendly_bets.repositories;

import net.friendly_bets.models.gameresults.GameResultRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GameResultRecordRepository extends MongoRepository<GameResultRecord, String> {

    List<GameResultRecord> findByLeagueCodeAndMatchdayAndSeason(
            String leagueCode, int matchday, String season);

    Optional<GameResultRecord> findByLeagueCodeAndMatchdayAndSeasonAndHomeTeamIdAndAwayTeamId(
            String leagueCode, int matchday, String season, String homeTeamId, String awayTeamId);
}
