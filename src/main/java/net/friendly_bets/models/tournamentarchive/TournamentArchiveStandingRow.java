package net.friendly_bets.models.tournamentarchive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TournamentArchiveStandingRow {

    @Field(name = "group")
    private String group;

    @Field(name = "rank")
    private int rank;

    @Field(name = "team_id")
    private String teamId;

    @Field(name = "played")
    private int played;

    @Field(name = "wins")
    private int wins;

    @Field(name = "draws")
    private int draws;

    @Field(name = "losses")
    private int losses;

    @Field(name = "goals_for")
    private int goalsFor;

    @Field(name = "goals_against")
    private int goalsAgainst;

    @Field(name = "goal_difference")
    private int goalDifference;

    @Field(name = "points")
    private int points;

    /** direct | best_third | eliminated */
    @Field(name = "qualification_status")
    private String qualificationStatus;
}
