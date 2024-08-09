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
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "seasons")
public class Season {

    public enum Status {
        CREATED, SCHEDULED, ACTIVE, PAUSED, FINISHED
    }

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "title")
    private String title;

    @Field(name = "bet_count_per_match_day")
    private Integer betCountPerMatchDay;

    @Field(name = "status")
    private Status status;

    @DBRef(lazy = true)
    @Field(name = "players")
    private List<User> players;

    @DBRef(lazy = true)
    @Field(name = "leagues")
    private List<League> leagues;
}
