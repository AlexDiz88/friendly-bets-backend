package net.friendly_bets.models.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MatchGoalEvent {

    /** Display minute as on the source page, e.g. "20", "90+2", "120+". */
    @Field(name = "minute")
    private String minute;

    /** Parsed minute for HT/FT/OT bucketing (injury time folded into base). */
    @Field(name = "minute_number")
    private Integer minuteNumber;

    /** HOME or AWAY. */
    @Field(name = "team_side")
    private String teamSide;

    @Field(name = "player_name")
    private String playerName;

    @Field(name = "is_penalty_shootout")
    private Boolean penaltyShootout;
}
