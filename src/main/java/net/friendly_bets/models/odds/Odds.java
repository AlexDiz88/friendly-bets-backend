package net.friendly_bets.models.odds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "odds")
public class Odds {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "match_schedule_id")
    private String matchScheduleId;

    /** Cached Marathonbet event treeId after successful match. */
    @Field(name = "marathonbet_tree_id")
    private Long marathonbetTreeId;

    @Field(name = "fetched_at")
    private Instant fetchedAt;

    @Field(name = "frozen_at")
    private Instant frozenAt;

    @Field(name = "bookmakers")
    @Builder.Default
    private List<String> bookmakers = new ArrayList<>();

    @Field(name = "market_groups")
    @Builder.Default
    private List<OddsMarketGroup> marketGroups = new ArrayList<>();
}
