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

    @Test
    void uniqueByEventId_keepsOneRowWhenSameEventOnAdjacentFeedDays() {
        Instant kickoff = Instant.parse("2026-11-24T20:00:00Z");
        FlashscoreParsedDayPage.Match fromNov24 = FlashscoreParsedDayPage.Match.builder()
                .eventId("vPVzwlTF")
                .homeName("Arsenal")
                .awayName("Dortmund")
                .utcKickoff(kickoff)
                .build();
        FlashscoreParsedDayPage.Match fromNov25 = FlashscoreParsedDayPage.Match.builder()
                .eventId("vPVzwlTF")
                .homeName("Arsenal")
                .awayName("Dortmund")
                .utcKickoff(kickoff)
                .build();
        FlashscoreParsedDayPage.Match other = FlashscoreParsedDayPage.Match.builder()
                .eventId("pxXsydY8")
                .homeName("RB Leipzig")
                .awayName("Lens")
                .utcKickoff(kickoff)
                .build();

        List<FlashscoreParsedDayPage.Match> deduped = FlashscoreFullMatchResolver.uniqueByEventId(
                List.of(fromNov24, other, fromNov25));

        assertEquals(2, deduped.size());
        assertEquals("vPVzwlTF", deduped.get(0).getEventId());
        assertEquals("pxXsydY8", deduped.get(1).getEventId());
    }
}
