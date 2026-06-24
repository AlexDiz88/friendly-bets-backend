package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wc26FifaStandingRowDto {

    private int rank;
    private String fifaCode;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
    /** W/D/L for last finished matches, oldest → newest. */
    private List<String> form;
    /** direct | best_third | eliminated | live */
    private String qualificationStatus;
    private boolean liveNow;
    /** Goals scored by this team in the current live match, when {@link #liveNow}. */
    private Integer liveMatchGoals;
    /** Full live match score (home:away), when {@link #liveNow}. */
    private String liveMatchScore;
}
