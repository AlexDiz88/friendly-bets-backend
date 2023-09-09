package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "player_stats")
public class PlayerStats {

    @MongoId
    @Field(name = "_id")
    private String id;

    private String seasonId;
    private String leagueId;

    @DBRef(lazy = true)
    private User user;

    private Integer totalBets;
    private Integer betCount;
    private Integer wonBetCount;
    private Integer returnedBetCount;
    private Integer lostBetCount;
    private Integer emptyBetCount;
    private Double winRate;
    private Double averageOdds;
    private Double averageWonBetOdds;
    private Double actualBalance;
    private Double sumOfOdds;
    private Double sumOfWonOdds;

    private void calculateWinRate() {
        if (betCount == 0 || betCount - returnedBetCount - emptyBetCount == 0) {
            winRate = 0.0;
        } else {
            winRate = wonBetCount * 100.0 / (betCount - returnedBetCount - emptyBetCount);
        }
    }

    private void calculateAverageOdds() {
        if (betCount == 0 || betCount - emptyBetCount == 0) {
            averageOdds = 0.0;
        } else {
            averageOdds = sumOfOdds / (betCount - emptyBetCount);
        }
    }

    private void calculateAverageWonBetOdds() {
        if (betCount == 0 || betCount - emptyBetCount == 0) {
            averageWonBetOdds = 0.0;
        } else {
            averageWonBetOdds = sumOfOdds / (betCount - emptyBetCount);
        }
    }
}
