package net.friendly_bets.repositories;

import net.friendly_bets.models.odds.OddsDemoSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OddsDemoSnapshotRepository extends MongoRepository<OddsDemoSnapshot, String> {

    List<OddsDemoSnapshot> findByLeagueSlugOrderByEventDateAsc(String leagueSlug);

    Optional<OddsDemoSnapshot> findByOddsApiEventId(Long oddsApiEventId);

    @Query(value = "{ 'odds_api_event_id': ?0 }", fields = "{ '_id': 1 }")
    Optional<OddsDemoSnapshot> findIdByOddsApiEventId(Long oddsApiEventId);

    void deleteByLeagueSlug(String leagueSlug);

    void deleteByOddsApiEventId(long oddsApiEventId);

    long countByLeagueSlug(String leagueSlug);
}
