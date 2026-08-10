package net.friendly_bets.models.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "match_schedules")
@CompoundIndexes({
        @CompoundIndex(
                name = "league_season_matchday_teams_unique",
                def = "{'league_id': 1, 'season_id': 1, 'matchday': 1, 'home_team_id': 1, 'away_team_id': 1}",
                unique = true
        ),
        @CompoundIndex(
                name = "season_league_matchday",
                def = "{'season_id': 1, 'league_id': 1, 'matchday': 1}"
        )
})
public class MatchSchedule {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "season_id")
    private String seasonId;

    @Field(name = "league_id")
    private String leagueId;

    @Field(name = "league_code")
    private String leagueCode;

    /** Slot order (1..N). */
    @Field(name = "matchday")
    private int matchday;

    /** Canonical Bet.match_day / ExpandedMatchdaySlot.id. */
    @Field(name = "slot_id")
    private String slotId;

    @Field(name = "home_team_id")
    private String homeTeamId;

    @Field(name = "away_team_id")
    private String awayTeamId;

    /** Absolute kickoff instant (BSON Date = UTC). */
    @Field(name = "utc_kickoff")
    private Instant utcKickoff;

    @Field(name = "status")
    private String status;

    @Field(name = "game_score")
    private GameScore gameScore;

    @Field(name = "live_minute")
    private Integer liveMinute;

    @Field(name = "live_minute_label")
    private String liveMinuteLabel;

    @Field(name = "goals")
    @Builder.Default
    private List<MatchGoalEvent> goals = new ArrayList<>();

    @Field(name = "stats")
    private MatchTeamStats stats;

    /** Announced injury/added time after first half (minutes); optional FULL_MATCH field. */
    @Field(name = "added_time_first_half")
    private Integer addedTimeFirstHalf;

    /** Announced injury/added time after second half (minutes); optional FULL_MATCH field. */
    @Field(name = "added_time_second_half")
    private Integer addedTimeSecondHalf;

    @Field(name = "finalized_at")
    private Instant finalizedAt;

    @Field(name = "finalized_by_provider")
    private String finalizedByProvider;

    @Field(name = "full_details_fetched_at")
    private Instant fullDetailsFetchedAt;

    /**
     * First time LIVE reported FINISHED for this schedule (UTC).
     * FULL_MATCH waits {@code match-result-sync.full-match-initial-delay-ms} after this.
     */
    @Field(name = "live_finished_detected_at")
    private Instant liveFinishedDetectedAt;

    /** Earliest Instant when orchestrator may call FULL again (deferral after not-ready). */
    @Field(name = "full_match_next_attempt_at")
    private Instant fullMatchNextAttemptAt;

    /** Consecutive FULL responses where provider status was still unfinished. */
    @Field(name = "full_match_not_ready_count")
    private Integer fullMatchNotReadyCount;

    @Field(name = "fetched_at")
    private Instant fetchedAt;
}
