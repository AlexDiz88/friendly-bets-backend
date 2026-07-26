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
public class MatchTeamStats {

    @Field(name = "possession_home")
    private Integer possessionHome;

    @Field(name = "possession_away")
    private Integer possessionAway;

    @Field(name = "shots_home")
    private Integer shotsHome;

    @Field(name = "shots_away")
    private Integer shotsAway;

    @Field(name = "shots_on_target_home")
    private Integer shotsOnTargetHome;

    @Field(name = "shots_on_target_away")
    private Integer shotsOnTargetAway;

    @Field(name = "yellow_cards_home")
    private Integer yellowCardsHome;

    @Field(name = "yellow_cards_away")
    private Integer yellowCardsAway;

    @Field(name = "xg_home")
    private Double xgHome;

    @Field(name = "xg_away")
    private Double xgAway;
}
