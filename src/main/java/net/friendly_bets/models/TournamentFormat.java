package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "tournament_formats")
public class TournamentFormat {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "format_code")
    private String formatCode;

    @Field(name = "name")
    private String name;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "regular_stage")
    private RoundRobinStage regularStage;

    @Field(name = "group_stage")
    private RoundRobinStage groupStage;

    @Field(name = "playoff")
    private List<PlayoffRound> playoff;
}
