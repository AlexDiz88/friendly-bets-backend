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
@Document(collection = "client_version")
public class ClientVersion {

    public static final String CURRENT_ID = "current";

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "build_id")
    private String buildId;

    @Field(name = "updated_at")
    private LocalDateTime updatedAt;
}
