package net.friendly_bets.repositories;

import net.friendly_bets.models.odds.OddsDemoSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OddsDemoSnapshotRepository extends MongoRepository<OddsDemoSnapshot, String> {

    List<OddsDemoSnapshot> findByLeagueSlugOrderByEventDateAsc(String leagueSlug);

    Optional<OddsDemoSnapshot> findByOddsApiEventId(Long oddsApiEventId);

    void deleteByLeagueSlug(String leagueSlug);
}
