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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * External ids by provider storage key (e.g. soccer365, 24score).
     * soccer365 game id from schedule {@code dt-id} is stored under {@code soccer365}.
     */
    @Field(name = "external_ids")
    @Builder.Default
    private Map<String, String> externalIds = new HashMap<>();

    @Field(name = "finalized_at")
    private Instant finalizedAt;

    @Field(name = "finalized_by_provider")
    private String finalizedByProvider;

    @Field(name = "full_details_fetched_at")
    private Instant fullDetailsFetchedAt;

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;

    public String externalId(String storageKey) {
        if (externalIds == null || storageKey == null) {
            return null;
        }
        return externalIds.get(storageKey);
    }

    public void putExternalId(String storageKey, String externalId) {
        if (storageKey == null || externalId == null || externalId.isBlank()) {
            return;
        }
        if (externalIds == null) {
            externalIds = new HashMap<>();
        }
        externalIds.put(storageKey, externalId);
    }
}
