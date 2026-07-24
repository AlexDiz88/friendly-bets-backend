package net.friendly_bets.models.tournamentarchive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TournamentArchiveBestThirdRow {

    @Field(name = "rank")
    private int rank;

    @Field(name = "group")
    private String group;

    @Field(name = "team_id")
    private String teamId;

    /** Только review/import; в Mongo не сохраняется. */
    @Transient
    private String fifaCode;

    @Field(name = "played")
    private int played;

    @Field(name = "wins")
    private int wins;

    @Field(name = "draws")
    private int draws;

    @Field(name = "losses")
    private int losses;

    @Field(name = "points")
    private int points;

    @Field(name = "goal_difference")
    private int goalDifference;

    @Field(name = "goals_for")
    private int goalsFor;

    @Field(name = "goals_against")
    private int goalsAgainst;

    @Field(name = "qualifies")
    private boolean qualifies;
}
