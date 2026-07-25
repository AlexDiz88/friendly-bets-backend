package net.friendly_bets.repositories;

import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MarathonbetSyncRunRepository extends MongoRepository<MarathonbetSyncRun, String> {
}
