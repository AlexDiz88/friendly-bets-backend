package net.friendly_bets.repositories;

import net.friendly_bets.models.ErrorLog;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

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

    /** Active rows only ({@code archived} missing/false/null). */
    @Aggregation(pipeline = {
            "{ $match: { archived: { $ne: true } } }",
            "{ $addFields: { sortAt: { $ifNull: ['$last_occurred_at', '$created_at'] } } }",
            "{ $sort: { sortAt: -1 } }",
            "{ $skip: ?0 }",
            "{ $limit: ?1 }"
    })
    List<ErrorLog> findRecentActive(int skip, int limit);

    long countByArchivedTrue();

    @Query("{ archived: { $ne: true } }")
    List<ErrorLog> findActive();

    @Query(value = "{ archived: { $ne: true } }", delete = true)
    long deleteActive();

    long deleteByArchivedTrue();

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
