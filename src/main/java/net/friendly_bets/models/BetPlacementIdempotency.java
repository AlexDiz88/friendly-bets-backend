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

@Document(collection = "bet_placement_idempotency")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetPlacementIdempotency {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "idempotency_key")
    private String idempotencyKey;

    @Field(name = "bet_id")
    private String betId;

    @Field(name = "created_at")
    private LocalDateTime createdAt;
}
