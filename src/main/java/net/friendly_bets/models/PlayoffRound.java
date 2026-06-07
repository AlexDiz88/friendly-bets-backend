package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PlayoffRound {

    @Field(name = "stage")
    private String stage;

    @Field(name = "matchday_count")
    private int matchdayCount;
}
