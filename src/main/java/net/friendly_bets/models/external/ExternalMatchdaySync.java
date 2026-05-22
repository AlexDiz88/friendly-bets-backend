package net.friendly_bets.models.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "external_matchday_sync")
@CompoundIndex(
        name = "comp_matchday_season_unique",
        def = "{'competition_code': 1, 'matchday': 1, 'season': 1}",
        unique = true
)
public class ExternalMatchdaySync {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "competition_code")
    private String competitionCode;

    @Field(name = "matchday")
    private int matchday;

    @Field(name = "season")
    private String season;

    @Field(name = "sync_status")
    private ExternalMatchdaySyncStatus syncStatus;

    @Field(name = "expected_match_count")
    private int expectedMatchCount;

    @Field(name = "finished_match_count")
    private int finishedMatchCount;

    @Field(name = "first_fetched_at")
    private LocalDateTime firstFetchedAt;

    @Field(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Field(name = "completed_at")
    private LocalDateTime completedAt;
}
