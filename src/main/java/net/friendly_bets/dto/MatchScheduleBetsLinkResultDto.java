package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScheduleBetsLinkResultDto {

    private String seasonId;
    private String leagueCode;
    private int schedulesProcessed;
    private int betsLinked;
    private int errors;
}
