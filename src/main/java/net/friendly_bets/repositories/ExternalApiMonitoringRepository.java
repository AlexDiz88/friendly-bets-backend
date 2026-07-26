package net.friendly_bets.repositories;

import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalApiMonitoringRepository extends MongoRepository<ExternalApiMonitoringRun, String> {

    List<ExternalApiMonitoringRun> findByLayerAndStartedAtAfterOrderByStartedAtDesc(
            ExternalDataLayer layer,
            LocalDateTime startedAtAfter,
            Pageable pageable
    );

    List<ExternalApiMonitoringRun> findByLayerOrderByStartedAtDesc(
            ExternalDataLayer layer,
            Pageable pageable
    );

    ExternalApiMonitoringRun findFirstByLayerOrderByStartedAtDesc(ExternalDataLayer layer);

    long deleteByLayer(ExternalDataLayer layer);
}
