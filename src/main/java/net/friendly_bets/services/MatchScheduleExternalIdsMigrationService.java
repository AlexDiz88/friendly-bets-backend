package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MatchScheduleExternalIdsMigrationResultDto;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * One-shot admin cleanup: remove obsolete {@code match_schedules.external_ids}.
 * Layers resolve events via {@code utc_kickoff} + provider aliases, not foreign match IDs.
 */
@Service
@RequiredArgsConstructor
public class MatchScheduleExternalIdsMigrationService {

    private static final String COLLECTION = "match_schedules";
    private static final String FIELD = "external_ids";

    private final MongoTemplate mongoTemplate;

    public MatchScheduleExternalIdsMigrationResultDto unsetExternalIds() {
        if (!mongoTemplate.collectionExists(COLLECTION)) {
            return MatchScheduleExternalIdsMigrationResultDto.builder()
                    .matched(0)
                    .modified(0)
                    .message("matchScheduleExternalIdsMigrationDone")
                    .build();
        }
        Query query = Query.query(Criteria.where(FIELD).exists(true));
        var result = mongoTemplate.updateMulti(query, new Update().unset(FIELD), COLLECTION);
        return MatchScheduleExternalIdsMigrationResultDto.builder()
                .matched(result.getMatchedCount())
                .modified(result.getModifiedCount())
                .message("matchScheduleExternalIdsMigrationDone")
                .build();
    }
}
