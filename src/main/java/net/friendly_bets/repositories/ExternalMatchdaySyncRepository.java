package net.friendly_bets.repositories;

import net.friendly_bets.models.external.ExternalMatchdaySync;
import net.friendly_bets.models.external.ExternalMatchdaySyncStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalMatchdaySyncRepository extends MongoRepository<ExternalMatchdaySync, String> {

    Optional<ExternalMatchdaySync> findByCompetitionCodeAndMatchdayAndSeason(
            String competitionCode, int matchday, String season);

    List<ExternalMatchdaySync> findBySyncStatus(ExternalMatchdaySyncStatus syncStatus);
}
