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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "odds_demo_snapshots")
public class OddsDemoSnapshot {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "odds_api_event_id")
    private Long oddsApiEventId;

    @Field(name = "home")
    private String home;

    @Field(name = "away")
    private String away;

    @Field(name = "event_date")
    private String eventDate;

    @Field(name = "league_slug")
    private String leagueSlug;

    @Field(name = "status")
    private String status;

    @Field(name = "bookmakers")
    private List<String> bookmakers;

    @Field(name = "merged_lines")
    @Builder.Default
    private List<MergedOddsLine> mergedLines = new ArrayList<>();

    @Field(name = "market_groups")
    @Builder.Default
    private List<OddsMarketGroup> marketGroups = new ArrayList<>();

    @Field(name = "raw_bookmakers")
    @Builder.Default
    private Map<String, List<OddsMarket>> rawBookmakers = new LinkedHashMap<>();

    @Field(name = "fetched_at")
    private LocalDateTime fetchedAt;
}
