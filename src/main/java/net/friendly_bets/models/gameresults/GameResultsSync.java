package net.friendly_bets.models.gameresults;

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
@Document(collection = "game_results_sync")
@CompoundIndex(
        name = "league_matchday_season_unique",
        def = "{'league_code': 1, 'matchday': 1, 'season': 1}",
        unique = true
)
public class GameResultsSync {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "matchday")
    private int matchday;

    @Field(name = "season")
    private String season;

    @Field(name = "sync_status")
    private GameResultsSyncStatus syncStatus;

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
