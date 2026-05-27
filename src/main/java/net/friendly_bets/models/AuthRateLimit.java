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
@Document(collection = "auth_rate_limits")
public class AuthRateLimit {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "attempts")
    private int attempts;

    @Field(name = "window_start")
    private LocalDateTime windowStart;
}
