package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameScoreFromGoalsTest {

    @Test
    @DisplayName("45+ and 90+ stay in HT / regulation FT, not OT")
    void stoppageTimeStaysInPeriod() {
        GameScore score = GameScoreFromGoals.from(List.of(
                goal("45+3", 45, "HOME", false),
                goal("90+6", 90, "AWAY", false),
                goal("46", 46, "HOME", false)
        ));
        assertEquals("1:0", score.getFirstTime());
        assertEquals("2:1", score.getFullTime());
        assertNull(score.getOverTime());
        assertNull(score.getPenalty());
    }

    @Test
    @DisplayName("OT goals produce cumulative overTime; PEN only from shootout flag")
    void extraTimeAndShootout() {
        GameScore score = GameScoreFromGoals.from(List.of(
                goal("20", 20, "HOME", false),
                goal("71", 71, "AWAY", false),
                goal("113", 113, "HOME", false),
                goal("117", 117, "AWAY", false),
                goal("120+", 120, "HOME", true),
                goal("120+", 120, "AWAY", true),
                goal("120+", 120, "HOME", true),
                goal("120+", 120, "AWAY", true),
                goal("120+", 120, "HOME", true),
                goal("120+", 120, "AWAY", true),
                goal("120+", 120, "AWAY", true)
        ));
        assertEquals("1:0", score.getFirstTime());
        assertEquals("1:1", score.getFullTime());
        assertEquals("2:2", score.getOverTime());
        assertEquals("3:4", score.getPenalty());
    }

    @Test
    @DisplayName("resolveBaseMinute reads leading number from label")
    void resolveBaseMinuteFromLabel() {
        assertEquals(45, GameScoreFromGoals.resolveBaseMinute(goal("45+3", null, "HOME", false)));
        assertEquals(90, GameScoreFromGoals.resolveBaseMinute(goal("90+6", 99, "HOME", false)));
        assertEquals(120, GameScoreFromGoals.resolveBaseMinute(goal("120+", 120, "HOME", false)));
    }

    private static MatchGoalEvent goal(String minute, Integer minuteNumber, String side, boolean penShootout) {
        return MatchGoalEvent.builder()
                .minute(minute)
                .minuteNumber(minuteNumber)
                .teamSide(side)
                .penaltyShootout(penShootout)
                .build();
    }
}
