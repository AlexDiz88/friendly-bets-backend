package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = false)
@Document(collection = "player_stats")
public class PlayerStats extends Stats {

    @MongoId
    @Field(name = "_id")
    private String id;
    private String seasonId;
    private String leagueId;
    @DBRef(lazy = true)
    private User user;
    private Integer totalBets;
}
