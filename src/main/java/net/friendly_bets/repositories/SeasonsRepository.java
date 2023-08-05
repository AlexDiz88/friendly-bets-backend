package net.friendly_bets.repositories;

import net.friendly_bets.models.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface SeasonsRepository extends JpaRepository<Season, Long> {

    boolean existsByTitle(String title);

    Season findByTitle(String title);

    Optional<Season> findSeasonByStatus(Season.Status status);

}
