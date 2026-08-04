package net.friendly_bets.repositories;

import net.friendly_bets.models.ErrorLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorLogRepository extends MongoRepository<ErrorLog, String> {

    List<ErrorLog> findTop200ByOrderByCreatedAtDesc();

    boolean existsByProviderAndCodeAndMatchScheduleId(String provider, String code, String matchScheduleId);

    Optional<ErrorLog> findFirstByProviderAndCodeAndMatchScheduleIdAndMessage(
            String provider,
            String code,
            String matchScheduleId,
            String message
    );

    Optional<ErrorLog> findFirstByProviderAndCodeAndLayerAndLeagueCodeAndMessage(
            String provider,
            String code,
            String layer,
            String leagueCode,
            String message
    );

    List<ErrorLog> findByCodeAndHomeTeam(String code, String homeTeam);

    List<ErrorLog> findByCodeAndAwayTeam(String code, String awayTeam);
}
