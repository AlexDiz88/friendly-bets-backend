package net.friendly_bets.repositories;

import net.friendly_bets.models.Season;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;


public interface SeasonsRepository extends MongoRepository<Season, String> {

    boolean existsByTitle(String title);

    Season findByTitleEquals(String title);

    Optional<Season> findSeasonByStatus(Season.Status status);
}
