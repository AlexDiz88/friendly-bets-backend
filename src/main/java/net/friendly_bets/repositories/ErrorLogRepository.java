package net.friendly_bets.repositories;

import net.friendly_bets.models.ErrorLog;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ErrorLogRepository extends MongoRepository<ErrorLog, String> {

    @Aggregation(pipeline = {
            "{ $addFields: { sortAt: { $ifNull: ['$last_occurred_at', '$created_at'] } } }",
            "{ $sort: { sortAt: -1 } }",
            "{ $skip: ?0 }",
            "{ $limit: ?1 }"
    })
    List<ErrorLog> findRecent(int skip, int limit);

    Optional<ErrorLog> findFirstByProviderAndCodeAndMatchScheduleId(String provider, String code, String matchScheduleId);

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
