package net.friendly_bets.models;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Document(collection = "accounts")
public class User {

    public enum Role {
        USER, MODERATOR, ADMIN
    }

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "created_at")
    private LocalDateTime createdAt;

    @Field(name = "email")
    private String email;

    @Field(name = "hash_password")
    private String hashPassword;

    @Field(name = "role")
    private Role role;

    @Field(name = "username")
    private String username;

    @Field(name = "avatar")
    private String avatar;
}
