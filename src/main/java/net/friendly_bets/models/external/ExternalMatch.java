package net.friendly_bets.models.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "external_matches")
@CompoundIndex(
        name = "comp_matchday_ext_unique",
        def = "{'competition_code': 1, 'matchday': 1, 'season': 1, 'external_match_id': 1}",
        unique = true
)
public class ExternalMatch {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "external_match_id")
    private long externalMatchId;

    @Field(name = "competition_code")
    private String competitionCode;

    @Field(name = "matchday")
    private int matchday;

    @Field(name = "season")
    private String season;

    @Field(name = "status")
    private String status;

    @Field(name = "utc_date")
    private LocalDateTime utcDate;

    @Field(name = "home_football_data_team_id")
    private int homeFootballDataTeamId;

    @Field(name = "away_football_data_team_id")
    private int awayFootballDataTeamId;

    @Field(name = "home_team_name")
    private String homeTeamName;

    @Field(name = "away_team_name")
    private String awayTeamName;

    /** Внутренний id команды Friendly Bets (если удалось сопоставить). */
    @Field(name = "home_team_id")
    private String homeTeamId;

    @Field(name = "away_team_id")
    private String awayTeamId;

    @Field(name = "league_id")
    private String leagueId;

    @Field(name = "game_score")
    private GameScore gameScore;

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Field(name = "api_last_updated")
    private LocalDateTime apiLastUpdated;
}
