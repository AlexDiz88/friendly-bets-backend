package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;

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
    @DBRef
    private List<Bet> bets;
}
