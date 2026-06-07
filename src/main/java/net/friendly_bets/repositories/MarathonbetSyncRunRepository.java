package net.friendly_bets.repositories;

import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MarathonbetSyncRunRepository extends MongoRepository<MarathonbetSyncRun, String> {

    Optional<MarathonbetSyncRun> findFirstByOrderByStartedAtDesc();
}
