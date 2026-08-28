package net.friendly_bets.flashscore;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlashscoreHttpClientTest {

    @Test
    void feedDayOffset_usesEditionTodayNotUtc() {
        LocalDate matchDay = LocalDate.of(2026, 8, 28);
        LocalDate almatyToday = LocalDate.of(2026, 8, 29);
        assertEquals(-1, FlashscoreHttpClient.feedDayOffset(matchDay, almatyToday));
        assertEquals(0, FlashscoreHttpClient.feedDayOffset(almatyToday, almatyToday));
        assertEquals(0, FlashscoreHttpClient.feedDayOffset(matchDay, matchDay));
    }
}
