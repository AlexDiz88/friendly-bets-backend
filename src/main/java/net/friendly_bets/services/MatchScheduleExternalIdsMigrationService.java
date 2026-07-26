package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MatchScheduleExternalIdsMigrationResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * One-shot admin script: unset obsolete {@code match_schedules.external_ids}.
 */
@Service
@RequiredArgsConstructor
public class MatchScheduleExternalIdsMigrationService {

    private static final Logger log = LoggerFactory.getLogger(MatchScheduleExternalIdsMigrationService.class);
    private static final String COLLECTION = "match_schedules";

    private final MongoTemplate mongoTemplate;

    public MatchScheduleExternalIdsMigrationResultDto migrate() {
        if (!mongoTemplate.collectionExists(COLLECTION)) {
            return MatchScheduleExternalIdsMigrationResultDto.builder()
                    .matched(0)
                    .modified(0)
                    .message("collectionMissing")
                    .build();
        }
        Query query = new Query(Criteria.where("external_ids").exists(true));
        long matched = mongoTemplate.count(query, COLLECTION);
        var result = mongoTemplate.updateMulti(query, new Update().unset("external_ids"), COLLECTION);
        long modified = result.getModifiedCount();
        log.info("Unset match_schedules.external_ids: matched={}, modified={}", matched, modified);
        return MatchScheduleExternalIdsMigrationResultDto.builder()
                .matched(matched)
                .modified(modified)
                .message("ok")
                .build();
    }
}
