package net.friendly_bets.ruscore;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuscoreFullMatchResolverTest {

    @Test
    void daysCoveringWindow_crossesMidnight() {
        Instant kickoff = Instant.parse("2026-08-10T01:00:00Z");
        List<LocalDate> days = RuscoreFullMatchResolver.daysCoveringWindow(kickoff, Duration.ofHours(12));
        assertTrue(days.contains(LocalDate.of(2026, 8, 9)));
        assertTrue(days.contains(LocalDate.of(2026, 8, 10)));
        assertEquals(2, days.size());
    }

    @Test
    void daysCoveringWindow_singleDay() {
        Instant kickoff = Instant.parse("2026-08-09T14:00:00Z");
        List<LocalDate> days = RuscoreFullMatchResolver.daysCoveringWindow(kickoff, Duration.ofHours(12));
        assertEquals(List.of(LocalDate.of(2026, 8, 9)), days);
    }
}
