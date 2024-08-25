package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GameweekStats {
    private String userId;
    private Double balanceChange;
    private Double totalBalance;
    private Integer positionAfterGameweek;
    private Integer positionChange;
}
