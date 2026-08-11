package net.friendly_bets.flashscore;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FlashscoreMatchMeta {

    String homeTeamName;
    String awayTeamName;
    String competitionName;
}
