package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

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
    private Instant createdAt;

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
    private BetTitle betTitle;

    @Field(name = "bet_odds")
    private Double betOdds;

    @Field(name = "bet_size")
    private Integer betSize;

    @Field(name = "bet_result_added_at")
    private Instant betResultAddedAt;

    @DBRef(lazy = true)
    @Field(name = "bet_result_added_by")
    private User betResultAddedBy;

    @Field(name = "game_result")
    private GameScore gameScore;

    @Field(name = "bet_status")
    private BetStatus betStatus;

    @Field(name = "balance_change")
    private Double balanceChange;

    @Field(name = "updated_at")
    private Instant updatedAt;

    @DBRef(lazy = true)
    @Field(name = "updated_by")
    private User updatedBy;

    @Indexed
    @Field(name = "calendar_node_id")
    private String calendarNodeId;

    @Field(name = "match_schedule_id")
    private String matchScheduleId;
}
