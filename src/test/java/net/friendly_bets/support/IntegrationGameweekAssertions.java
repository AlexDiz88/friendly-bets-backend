package net.friendly_bets.support;

import lombok.experimental.UtilityClass;
import net.friendly_bets.models.CalendarNode;
import net.friendly_bets.models.GameweekStats;
import net.friendly_bets.repositories.CalendarsRepository;

import static org.junit.jupiter.api.Assertions.*;

@UtilityClass
public class IntegrationGameweekAssertions {

    public static CalendarNode reloadCalendar(CalendarsRepository repository, String calendarNodeId) {
        return repository.findById(calendarNodeId).orElseThrow(
                () -> new AssertionError("CalendarNode not found: " + calendarNodeId));
    }

    public static void assertGameweekOpen(CalendarNode calendarNode) {
        assertFalse(Boolean.TRUE.equals(calendarNode.getIsFinished()), "gameweek should not be finished");
        assertTrue(
                calendarNode.getGameweekStats() == null || calendarNode.getGameweekStats().isEmpty(),
                "gameweek stats should be empty while gameweek is open"
        );
    }

    public static void assertGameweekFinished(CalendarNode calendarNode, int expectedPlayersInStats) {
        assertTrue(Boolean.TRUE.equals(calendarNode.getIsFinished()), "gameweek should be finished");
        assertNotNull(calendarNode.getGameweekStats());
        assertEquals(expectedPlayersInStats, calendarNode.getGameweekStats().size(), "gameweek stats count");
    }

    public static GameweekStats findGameweekStats(CalendarNode calendarNode, String userId) {
        return calendarNode.getGameweekStats().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("GameweekStats not found for user " + userId));
    }

    public static void assertGameweekBalance(CalendarNode calendarNode,
                                             String userId,
                                             double expectedBalanceChange,
                                             double expectedTotalBalance) {
        GameweekStats stats = findGameweekStats(calendarNode, userId);
        assertEquals(expectedBalanceChange, stats.getBalanceChange(), 0.001, "balanceChange");
        assertEquals(expectedTotalBalance, stats.getTotalBalance(), 0.001, "totalBalance");
    }
}
