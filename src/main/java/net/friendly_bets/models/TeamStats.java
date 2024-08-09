package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TeamStats {

    @DBRef(lazy = true)
    private Team team;
    private Integer betCount;
    private Integer wonBetCount;
    private Integer returnedBetCount;
    private Integer lostBetCount;
    private Double winRate;
    private Double averageOdds;
    private Double averageWonBetOdds;
    private Double actualBalance;
    private Double sumOfOdds;
    private Double sumOfWonOdds;

    public void calculateWinRate() {
        if (betCount == 0 || betCount - returnedBetCount == 0) {
            winRate = 0.0;
        } else {
            winRate = wonBetCount * 100.0 / (betCount - returnedBetCount);
        }
    }

    public void calculateAverageOdds() {
        if (betCount == 0) {
            averageOdds = 0.0;
        } else {
            averageOdds = sumOfOdds == null ? 0.0 : sumOfOdds / betCount;
        }
    }

    public void calculateAverageWonBetOdds() {
        if (wonBetCount == 0) {
            averageWonBetOdds = 0.0;
        } else {
            averageWonBetOdds = sumOfWonOdds == null ? 0.0 : sumOfWonOdds / wonBetCount;
        }
    }
}
