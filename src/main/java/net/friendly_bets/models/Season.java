package net.friendly_bets.models;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Document(collection = "seasons")
public class Season {

    @MongoId
    @Field(name = "_id")
    private String id;
    @Field(name = "created_at")
    private LocalDateTime createdAt;
    @Field(name = "title")
    private String title;
    @Field(name = "leagues")
    private List<String> leagues;
    @Field(name = "bet_count_per_match_day")
    private Integer betCountPerMatchDay;
    @Field(name = "players")
    private List<User> players;
    @Field(name = "bets")
    private List<Bet> bets;
}
