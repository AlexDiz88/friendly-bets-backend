package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarkFinishedFullDetailsResultDto;
import net.friendly_bets.models.schedule.MatchSchedule;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.result.UpdateResult;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MatchScheduleMaintenanceService {

    private final MongoTemplate mongoTemplate;

    /**
     * Sets {@code full_details_fetched_at} (UTC Instant) on every {@code match_schedules}
     * document with {@code status = "FINISHED"} so FULL_MATCH skips them.
     */
    @Transactional
    public MarkFinishedFullDetailsResultDto markFinishedFullDetailsFetched() {
        Instant now = Instant.now();
        Query query = new Query(Criteria.where("status").is("FINISHED"));
        Update update = new Update().set("full_details_fetched_at", now);
        UpdateResult result = mongoTemplate.updateMulti(query, update, MatchSchedule.class);
        return MarkFinishedFullDetailsResultDto.builder()
                .matchedCount(result.getMatchedCount())
                .modifiedCount(result.getModifiedCount())
                .fullDetailsFetchedAt(now)
                .build();
    }
}
