package net.friendly_bets.providers.odds;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsRefreshSupportTest {

    @Test
    void needsRefresh_trueWhenOddsMissing() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
        assertTrue(OddsRefreshSupport.needsRefresh(match, false, Instant.now(), 48));
    }

    @Test
    void needsRefresh_falseWhenOddsExistAndKickoffFartherThanWindow() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.plus(3, ChronoUnit.DAYS))
                .build();
        assertFalse(OddsRefreshSupport.needsRefresh(match, true, now, 48));
    }

    @Test
    void needsRefresh_trueWhenOddsExistAndKickoffWithinWindow() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.plus(24, ChronoUnit.HOURS))
                .build();
        assertTrue(OddsRefreshSupport.needsRefresh(match, true, now, 48));
    }

    @Test
    void needsRefresh_falseWhenMatchAlreadyStarted() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.minus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(OddsRefreshSupport.needsRefresh(match, false, now, 48));
    }

    @Test
    void needsRefresh_falseWhenFinalized() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("FINISHED")
                .utcKickoff(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(OddsRefreshSupport.needsRefresh(match, false, Instant.now(), 48));
    }
}
