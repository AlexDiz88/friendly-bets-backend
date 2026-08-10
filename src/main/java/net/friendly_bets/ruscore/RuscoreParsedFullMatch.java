package net.friendly_bets.ruscore;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.models.schedule.MatchTeamStats;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class RuscoreParsedFullMatch {

    String eventId;
    String slug;
    String statusText;
    String homeTeamName;
    String awayTeamName;
    String competitionName;
    GameScore gameScore;
    @Builder.Default
    List<MatchGoalEvent> goals = new ArrayList<>();
    MatchTeamStats stats;
    Integer addedTimeFirstHalf;
    Integer addedTimeSecondHalf;
}
