package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "player_stats_by_bet_titles")
public class PlayerStatsByBetTitles {

    @MongoId
    @Field(name = "_id")
    private String id;
    private String seasonId;
    private String leagueId;
    private String userId;
    private List<BetTitleCategoryStats> betTitleCategoryStats;
}
