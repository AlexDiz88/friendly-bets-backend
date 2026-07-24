package net.friendly_bets.repositories;

import net.friendly_bets.models.schedule.PlayoffBracket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PlayoffBracketRepository extends MongoRepository<PlayoffBracket, String> {

    Optional<PlayoffBracket> findBySeasonIdAndLeagueId(String seasonId, String leagueId);
}
