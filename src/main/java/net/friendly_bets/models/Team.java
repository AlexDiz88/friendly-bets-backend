package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Document(collection = "teams")
public class Team {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Уникальный ключ: PascalCase без пробелов ({@code ManchesterUnited}).
     * i18n {@code teams:{title}}, локальный логотип {@code /upload/logo/{snake_case}.png}.
     */
    @Field(name = "title")
    private String title;

    @Field(name = "country")
    private String country;

    @Field(name = "logo")
    private String logo;

    @Field(name = "display_names")
    private TeamDisplayNames displayNames;

    @Field(name = "external_aliases")
    @Builder.Default
    private List<TeamExternalAlias> externalAliases = new ArrayList<>();

    @Field(name = "football_data_team_id")
    private Integer footballDataTeamId;

}
