package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@Document(collection = "game_result_odds")
@CompoundIndex(
        name = "game_result_bookmaker_unique",
        def = "{'game_result_id': 1, 'bookmaker': 1}",
        unique = true
)
public class GameResultOdds {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "game_result_id")
    private String gameResultId;

    @Field(name = "bookmaker")
    private String bookmaker;

    @Field(name = "odds_api_event_id")
    private Long oddsApiEventId;

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;

    @Field(name = "markets")
    @Builder.Default
    private List<OddsMarket> markets = new ArrayList<>();
}
