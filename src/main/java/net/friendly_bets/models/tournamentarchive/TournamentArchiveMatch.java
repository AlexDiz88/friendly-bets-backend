package net.friendly_bets.models.tournamentarchive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.GameScore;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TournamentArchiveMatch {

    @Field(name = "match_number")
    private int matchNumber;

    @Field(name = "stage")
    private String stage;

    @Field(name = "group")
    private String group;

    @Field(name = "kickoff_utc")
    private LocalDateTime kickoffUtc;

    @Field(name = "home_team_id")
    private String homeTeamId;

    @Field(name = "away_team_id")
    private String awayTeamId;

    @Field(name = "game_score")
    private GameScore gameScore;

    @Field(name = "winner_team_id")
    private String winnerTeamId;
}
