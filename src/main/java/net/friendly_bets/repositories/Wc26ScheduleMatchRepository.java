package net.friendly_bets.repositories;

import net.friendly_bets.models.wc26.Wc26ScheduleMatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface Wc26ScheduleMatchRepository extends MongoRepository<Wc26ScheduleMatch, String> {

    boolean existsByScheduleId(int scheduleId);

    Optional<Wc26ScheduleMatch> findByScheduleId(int scheduleId);

    Optional<Wc26ScheduleMatch> findByHomeFifaAndAwayFifa(String homeFifa, String awayFifa);

    List<Wc26ScheduleMatch> findAllByOrderByKickoffUtcAsc();
}
