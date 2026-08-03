package net.friendly_bets.providers;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import net.friendly_bets.models.schedule.MatchTeamStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider-agnostic FULL_MATCH payload mapped onto {@code match_schedules}.
 */
@Value
@Builder
public class FullMatchDetails {

    GameScore gameScore;
    @Builder.Default
    List<MatchGoalEvent> goals = new ArrayList<>();
    MatchTeamStats stats;
    /** Optional provider status label; must map to finished via FullMatchStatusSupport before persist. */
    String statusText;
}
