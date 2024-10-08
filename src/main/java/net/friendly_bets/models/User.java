package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
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

    @Field(name = "email_is_confirmed")
    private Boolean emailIsConfirmed;

    @Field(name = "hash_password")
    private String hashPassword;

    @Field(name = "role")
    private Role role;

    @Field(name = "username")
    private String username;

    @Field(name = "avatar")
    private Binary avatar;

    @Field(name = "language")
    private String language;
}
