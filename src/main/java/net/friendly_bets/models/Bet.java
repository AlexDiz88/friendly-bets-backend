package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "bets")
public class Bet {
    public enum BetStatus {
        OPENED, WON, RETURNED, LOST, BLANK
    }

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @DBRef(lazy = true)
    @Field(name = "user")
    private User user;

    @Field(name = "season_id")
    private String seasonId;

    @DBRef(lazy = true)
    @Field(name = "league")
    private League league;

    @Field(name = "match_day")
    private String matchDay;

    @Field(name = "game_id")
    private Integer gameId;

    @Field(name = "game_date")
    private LocalDateTime gameDate;

    @Field(name = "home_team")
    private String homeTeam;

    @Field(name = "away_team")
    private String awayTeam;

    @Field(name = "bet_title")
    private String betTitle;

    @Field(name = "bet_odds")
    private Double betOdds;

    @Field(name = "bet_size")
    private Integer betSize;

    @Field(name = "game_result")
    private String gameResult;

    @Field(name = "bet_status")
    private BetStatus betStatus;

    @Field(name = "balance_change")
    private Double balanceChange;
}
