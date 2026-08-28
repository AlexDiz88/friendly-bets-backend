package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashscoreFullMatchResolverTest {

    @Test
    void daysCoveringWindow_eplEveningUsesFeedZoneCalendar() {
        Instant kickoff = Instant.parse("2026-08-28T19:00:00Z");
        List<LocalDate> days = FlashscoreFullMatchResolver.daysCoveringWindow(
                kickoff, Duration.ofHours(12), ZoneId.of("Asia/Almaty"));
        assertTrue(days.contains(LocalDate.of(2026, 8, 28)));
        assertTrue(days.contains(LocalDate.of(2026, 8, 29)));
        assertEquals(2, days.size());
    }
}
