package net.friendly_bets.models;

import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Document(collection = "images")
public class Image {
    @MongoId
    @Field(name = "_id")
    private String id;
    private Binary content;
    private String relatedObjectId;  // ID связанного объекта (новости, события и т.д.)
    private String relatedObjectType; // need??? Тип объекта, с которым связано изображение (например, новость)
    private String filename;
    private String contentType;
    private LocalDateTime uploadDate;
}
