package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchdaySettleResultDto {

    private int matchesSubmitted;
    private int betsProcessed;
    private boolean gameweekStatsRecalculated;
}
