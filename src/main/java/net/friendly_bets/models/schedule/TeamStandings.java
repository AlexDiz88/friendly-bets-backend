package net.friendly_bets.models.schedule;

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
@Document(collection = "team_standings")
@CompoundIndex(name = "season_league", def = "{'season_id': 1, 'league_id': 1}", unique = true)
public class TeamStandings {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "season_id")
    private String seasonId;

    @Field(name = "league_id")
    private String leagueId;

    @Field(name = "group")
    private String group;

    @Field(name = "rows")
    @Builder.Default
    private List<TeamStandingRow> rows = new ArrayList<>();

    @Field(name = "updated_at")
    private LocalDateTime updatedAt;
}
