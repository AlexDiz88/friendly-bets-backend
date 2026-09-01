package net.friendly_bets.repositories;

import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ExternalApiMonitoringRepository extends MongoRepository<ExternalApiMonitoringRun, String> {

    List<ExternalApiMonitoringRun> findByLayerAndStartedAtAfterOrderByStartedAtDesc(
            ExternalDataLayer layer,
            Instant startedAtAfter,
            Pageable pageable
    );

    List<ExternalApiMonitoringRun> findByLayerAndStatusInAndStartedAtAfterOrderByStartedAtDesc(
            ExternalDataLayer layer,
            List<ExternalApiMonitoringStatus> statuses,
            Instant startedAtAfter,
            Pageable pageable
    );

    long countByLayerAndStatusInAndStartedAtAfter(
            ExternalDataLayer layer,
            List<ExternalApiMonitoringStatus> statuses,
            Instant startedAtAfter
    );

    List<ExternalApiMonitoringRun> findByLayerOrderByStartedAtDesc(
            ExternalDataLayer layer,
            Pageable pageable
    );

    ExternalApiMonitoringRun findFirstByLayerOrderByStartedAtDesc(ExternalDataLayer layer);

    long countByLayerAndStartedAtAfter(ExternalDataLayer layer, Instant startedAtAfter);

    long countByLayerAndStatusAndStartedAtAfter(
            ExternalDataLayer layer,
            ExternalApiMonitoringStatus status,
            Instant startedAtAfter
    );

    long deleteByLayer(ExternalDataLayer layer);
}
