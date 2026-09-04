package net.friendly_bets.eurofootball;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EuroFootballFeedDatesTest {

    @Test
    void onlinePath_mapsRelativeOffsets() {
        LocalDate today = LocalDate.of(2026, 9, 5);
        assertEquals("/online/today", EuroFootballFeedDates.onlinePathForFeedDate(today, today));
        assertEquals("/online/yesterday", EuroFootballFeedDates.onlinePathForFeedDate(today.minusDays(1), today));
        assertEquals("/online/before-yesterday", EuroFootballFeedDates.onlinePathForFeedDate(today.minusDays(2), today));
        assertEquals("/online/tomorrow", EuroFootballFeedDates.onlinePathForFeedDate(today.plusDays(1), today));
        assertEquals("/online/after-tomorrow", EuroFootballFeedDates.onlinePathForFeedDate(today.plusDays(2), today));
    }

    @Test
    void onlinePath_rejectsFarOffset() {
        LocalDate today = LocalDate.of(2026, 9, 5);
        assertThrows(RuntimeException.class,
                () -> EuroFootballFeedDates.onlinePathForFeedDate(today.minusDays(3), today));
    }

    @Test
    void feedDate_usesMoscowCalendar() {
        // 19:00 UTC 4 Sep = 22:00 Moscow 4 Sep
        Instant kickoff = Instant.parse("2026-09-04T19:00:00Z");
        assertEquals(LocalDate.of(2026, 9, 4), EuroFootballFeedDates.feedDate(kickoff));
        // 22:30 UTC 4 Sep = 01:30 Moscow 5 Sep
        Instant late = Instant.parse("2026-09-04T22:30:00Z");
        assertEquals(LocalDate.of(2026, 9, 5), EuroFootballFeedDates.feedDate(late));
    }
}
