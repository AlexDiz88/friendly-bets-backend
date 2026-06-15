package net.friendly_bets.twentyfourscore;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
@Builder
public class TwentyFourScoreListMatch {

    long externalMatchId;
    String matchPath;
    String homeTeamName;
    String awayTeamName;
    LocalDate matchDate;
    LocalTime kickoffTime;
    Integer matchday;
    String fullTimeScore;
    String firstHalfScore;
    String extraTimeScore;
    String penaltyScore;
    String statusText;
    String liveMinuteLabel;
}
