package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "player_stats_by_teams")
public class PlayerStatsByTeams {

    @MongoId
    @Field(name = "_id")
    private String id;
    private String seasonId;
    private String leagueId;
    private String leagueNameRu;
    @DBRef
    private User user;
    private boolean isLeagueStats;
    private List<TeamStats> teamStats;
}
