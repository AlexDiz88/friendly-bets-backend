package net.friendly_bets.oddsapi;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OddsMatchContext {

    String homeTeamName;
    String awayTeamName;

    public static OddsMatchContext of(String home, String away) {
        return OddsMatchContext.builder()
                .homeTeamName(home)
                .awayTeamName(away)
                .build();
    }
}
