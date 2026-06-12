package net.friendly_bets.fourscore;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class FourScoreEventDetails {
    String eventSlug;
    String eventPath;
    String homeTeamName;
    String awayTeamName;
    String statusText;
    Integer headerHomeScore;
    Integer headerAwayScore;
    String firstHalfScore;
    String secondHalfScore;
    String extraTimeScore;
    String penaltyScore;
    LocalDateTime kickoffAt;
    FourScoreLeagueSection section;
}
