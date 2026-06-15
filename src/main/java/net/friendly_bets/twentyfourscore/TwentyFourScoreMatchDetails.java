package net.friendly_bets.twentyfourscore;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TwentyFourScoreMatchDetails {

    long externalMatchId;
    String matchPath;
    String homeTeamName;
    String awayTeamName;
    String statusText;
    String fullTimeScore;
    String firstHalfScore;
    String extraTimeScore;
    String penaltyScore;
    String liveMinuteLabel;
    Integer matchday;
    LocalDateTime kickoffAt;
}
