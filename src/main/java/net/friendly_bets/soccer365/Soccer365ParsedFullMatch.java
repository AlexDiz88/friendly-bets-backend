package net.friendly_bets.soccer365;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.models.schedule.MatchTeamStats;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class Soccer365ParsedFullMatch {

    String statusText;
    String homeTeamName;
    String awayTeamName;
    String competitionName;
    GameScore gameScore;
    @Builder.Default
    List<MatchGoalEvent> goals = new ArrayList<>();
    MatchTeamStats stats;
}
