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

    /** Goal from a penalty kick during the match (not shootout). */
    @Field(name = "is_penalty")
    private Boolean penalty;

    /** Goal in a penalty shootout after ET. */
    @Field(name = "is_penalty_shootout")
    private Boolean penaltyShootout;

    @Field(name = "is_own_goal")
    private Boolean ownGoal;

    /** Missed penalty (in-match or shootout); does not count toward score. */
    @Field(name = "is_missed")
    private Boolean missed;

    /** Goal disallowed by VAR; does not count toward score. */
    @Field(name = "is_var_disallowed")
    private Boolean varDisallowed;

    /** Flashscore ILX reason token, e.g. {@code offside}, {@code foul}, {@code handball}. */
    @Field(name = "var_disallowed_reason")
    private String varDisallowedReason;

    /** Straight red or second yellow shown as a red-card event (not a goal). */
    @Field(name = "is_red_card")
    private Boolean redCard;

    /** Second yellow that produced a red; implies {@link #redCard}. */
    @Field(name = "is_second_yellow")
    private Boolean secondYellow;
}
