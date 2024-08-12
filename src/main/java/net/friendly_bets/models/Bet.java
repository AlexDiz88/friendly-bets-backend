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
        OPENED, WON, RETURNED, LOST, EMPTY, DELETED
    }

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @DBRef(lazy = true)
    @Field(name = "created_by")
    private User createdBy;

    @DBRef(lazy = true)
    @Field(name = "user")
    private User user;

    @DBRef(lazy = true)
    @Field(name = "season")
    private Season season;

    @DBRef(lazy = true)
    @Field(name = "league")
    private League league;

    @Field(name = "match_day")
    private String matchDay;

    @DBRef(lazy = true)
    @Field(name = "home_team")
    private Team homeTeam;

    @DBRef(lazy = true)
    @Field(name = "away_team")
    private Team awayTeam;

    @Field(name = "bet_title")
    private String betTitle;

    @Field(name = "bet_odds")
    private Double betOdds;

    @Field(name = "bet_size")
    private Integer betSize;

    @Field(name = "bet_result_added_at")
    private LocalDateTime betResultAddedAt;

    @DBRef(lazy = true)
    @Field(name = "bet_result_added_by")
    private User betResultAddedBy;

    @Field(name = "game_result")
    private String gameResult;

    @Field(name = "bet_status")
    private BetStatus betStatus;

    @Field(name = "balance_change")
    private Double balanceChange;

    @Field(name = "updated_at")
    private LocalDateTime updatedAt;

    @DBRef(lazy = true)
    @Field(name = "updated_by")
    private User updatedBy;

    @Field(name = "calendar_node_id")
    private String calendarNodeId;
}
