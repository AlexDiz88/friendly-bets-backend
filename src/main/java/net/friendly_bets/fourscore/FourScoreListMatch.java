package net.friendly_bets.fourscore;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class FourScoreListMatch {
    FourScoreLeagueSection section;
    String eventSlug;
    String eventPath;
    String homeTeamName;
    String awayTeamName;
    String statusText;
    Integer homeScore;
    Integer awayScore;
    LocalTime kickoffTime;
    Long externalEventId;

    public boolean isTerminal() {
        if (statusText == null) {
            return false;
        }
        String s = statusText.trim().toLowerCase();
        return s.contains("завершено");
    }
}
