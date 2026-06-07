package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "account_tokens")
@CompoundIndex(name = "token_hash_type_idx", def = "{'token_hash': 1, 'type': 1}")
public class AccountToken {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "token_hash")
    private String tokenHash;

    @Field(name = "type")
    private AccountTokenType type;

    @Indexed(expireAfterSeconds = 0)
    @Field(name = "expires_at")
    private LocalDateTime expiresAt;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "used_at")
    private LocalDateTime usedAt;
}
