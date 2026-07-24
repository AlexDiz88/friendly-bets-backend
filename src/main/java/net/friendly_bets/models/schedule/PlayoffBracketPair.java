package net.friendly_bets.models.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PlayoffBracketPair {

    @Field(name = "match_number")
    private Integer matchNumber;

    @Field(name = "home_from_match")
    private Integer homeFromMatch;

    @Field(name = "away_from_match")
    private Integer awayFromMatch;

    @Field(name = "home_team_id")
    private String homeTeamId;

    @Field(name = "away_team_id")
    private String awayTeamId;

    @Field(name = "stage")
    private String stage;
}
