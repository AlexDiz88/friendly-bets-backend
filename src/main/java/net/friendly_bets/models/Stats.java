package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public abstract class Stats {

    protected Integer betCount;
    protected Integer wonBetCount;
    protected Integer returnedBetCount;
    protected Integer lostBetCount;
    protected Integer emptyBetCount;
    protected Double winRate;
    protected Double averageOdds;
    protected Double averageWonBetOdds;
    protected Double actualBalance;
    protected Double sumOfOdds;
    protected Double sumOfWonOdds;

    public void calculateWinRate() {
        if (betCount == 0 || betCount - returnedBetCount - emptyBetCount == 0) {
            winRate = 0.0;
        } else {
            winRate = wonBetCount * 100.0 / (betCount - returnedBetCount - emptyBetCount);
        }
    }

    public void calculateAverageOdds() {
        if (betCount == 0 || betCount - emptyBetCount == 0) {
            averageOdds = 0.0;
        } else {
            averageOdds = sumOfOdds == null ? 0.0 : sumOfOdds / (betCount - emptyBetCount);
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
