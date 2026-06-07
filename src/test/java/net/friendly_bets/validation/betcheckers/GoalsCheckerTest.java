package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.enums.BetTitleCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalsCheckerTest {

    private final GoalsChecker checker = new GoalsChecker();

    @Test
    void anyTeamWillScore_wonWhenAtLeastOneGoal() {
        assertEquals(BetStatus.WON, checker.check(score(1, 0), BetTitleCode.ANY_TEAM_WILL_SCORE));
        assertEquals(BetStatus.WON, checker.check(score(0, 2), BetTitleCode.ANY_TEAM_WILL_SCORE));
        assertEquals(BetStatus.WON, checker.check(score(2, 2), BetTitleCode.ANY_TEAM_WILL_SCORE));
    }

    @Test
    void anyTeamWillScore_lostOnNilNil() {
        assertEquals(BetStatus.LOST, checker.check(score(0, 0), BetTitleCode.ANY_TEAM_WILL_SCORE));
    }

    private static GameScore score(int home, int away) {
        return GameScore.builder()
                .fullTime(home + ":" + away)
                .firstTime("0:0")
                .build();
    }
}
