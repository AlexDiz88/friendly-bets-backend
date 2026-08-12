package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.TeamStandingRow;
import net.friendly_bets.utils.TeamTitleUtils;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LeagueStandingRowDto {

    private int rank;
    private String teamId;
    private String teamTitle;
    private String logoKey;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
    private String zoneCode;

    public static LeagueStandingRowDto from(TeamStandingRow row, Team team) {
        if (row == null) {
            return null;
        }
        return LeagueStandingRowDto.builder()
                .rank(row.getRank())
                .teamId(row.getTeamId())
                .teamTitle(team != null ? TeamTitleUtils.effectiveTitle(team) : null)
                .logoKey(team != null ? team.getLogo() : null)
                .played(row.getPlayed())
                .wins(row.getWins())
                .draws(row.getDraws())
                .losses(row.getLosses())
                .goalsFor(row.getGoalsFor())
                .goalsAgainst(row.getGoalsAgainst())
                .goalDifference(row.getGoalDifference())
                .points(row.getPoints())
                .zoneCode(row.getZoneCode())
                .build();
    }
}
