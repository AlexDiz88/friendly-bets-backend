package net.friendly_bets.models.wc26;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "wc26_schedule")
public class Wc26ScheduleMatch {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Indexed(unique = true)
    @Field(name = "schedule_id")
    private int scheduleId;

    @Field(name = "date")
    private String date;

    @Field(name = "time_local")
    private String timeLocal;

    @Field(name = "venue_key")
    private String venueKey;

    @Field(name = "stage")
    private String stage;

    @Field(name = "group")
    private String group;

    @Field(name = "home_fifa")
    private String homeFifa;

    @Field(name = "away_fifa")
    private String awayFifa;

    @Field(name = "label_key")
    private String labelKey;

    @Field(name = "kickoff_utc")
    private LocalDateTime kickoffUtc;
}
