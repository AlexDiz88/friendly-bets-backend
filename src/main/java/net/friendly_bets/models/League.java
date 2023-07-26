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
@Document(collection = "leagues")
public class League {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "league_name")
    private String name;

    @Field(name = "display_name_ru")
    private String displayNameRu;

    @Field(name = "display_name_en")
    private String displayNameEn;

    @DBRef(lazy = true)
    @Field(name = "teams")
    private List<Team> teams;

    @DBRef(lazy = true)
    @Field(name = "bets")
    private List<Bet> bets;
}
