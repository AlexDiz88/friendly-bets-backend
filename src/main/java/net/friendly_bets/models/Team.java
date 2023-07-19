package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

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

    @Field(name = "full_title_ru")
    private String fullTitleRu;

    @Field(name = "full_title_en")
    private String fullTitleEn;

    @Field(name = "country")
    private String country;

    @Field(name = "logo")
    private String logo;

}
