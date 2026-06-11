package net.friendly_bets.repositories;

import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarathonbetSyncRunRepository extends MongoRepository<MarathonbetSyncRun, String> {

    Optional<MarathonbetSyncRun> findFirstByOrderByStartedAtDesc();

    List<MarathonbetSyncRun> findByStartedAtAfterOrderByStartedAtDesc(LocalDateTime startedAt, Pageable pageable);
}
