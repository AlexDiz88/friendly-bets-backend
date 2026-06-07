package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "game_result_merged_odds")
public class GameResultMergedOdds {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "game_result_id")
    private String gameResultId;

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Field(name = "frozen_at")
    private LocalDateTime frozenAt;

    @Field(name = "bookmakers")
    @Builder.Default
    private List<String> bookmakers = new ArrayList<>();

    @Field(name = "market_groups")
    @Builder.Default
    private List<OddsMarketGroup> marketGroups = new ArrayList<>();
}
