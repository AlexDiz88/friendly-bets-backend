package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "calendar_nodes")
@CompoundIndex(def = "{'seasonId': 1, 'hasBets': 1}")
public class CalendarNode {
    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "season_id")
    private String seasonId;

    @Field(name = "start_date")
    private LocalDate startDate;

    @Field(name = "end_date")
    private LocalDate endDate;

    @Field(name = "league_matchday_nodes")
    private List<LeagueMatchdayNode> leagueMatchdayNodes;

    @Field(name = "has_bets")
    private Boolean hasBets;
}
