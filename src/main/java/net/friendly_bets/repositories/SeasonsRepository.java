package net.friendly_bets.repositories;

import net.friendly_bets.models.Season;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface SeasonsRepository extends MongoRepository<Season, String> {

    boolean existsByTitle(String title);

    Season findByTitle(String title);

    Optional<Season> findSeasonByStatus(Season.Status status);

}
