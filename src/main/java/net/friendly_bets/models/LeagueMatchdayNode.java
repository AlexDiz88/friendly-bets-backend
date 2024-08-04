package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LeagueMatchdayNode {
    private String leagueId;
    private League.LeagueCode leagueCode;
    private String matchDay;
    private Boolean isPlayoff;
    private String playoffRound;
}
