package net.friendly_bets.marathonbet;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarathonbetSyncBatchSupportTest {

    @Test
    void needsSseRefresh_trueWhenOddsMissing() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
        assertTrue(MarathonbetSyncBatchSupport.needsSseRefresh(match, false, Instant.now(), 48));
    }

    @Test
    void needsSseRefresh_falseWhenOddsExistAndKickoffFartherThanWindow() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.plus(3, ChronoUnit.DAYS))
                .build();
        assertFalse(MarathonbetSyncBatchSupport.needsSseRefresh(match, true, now, 48));
    }

    @Test
    void needsSseRefresh_trueWhenOddsExistAndKickoffWithinWindow() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.plus(24, ChronoUnit.HOURS))
                .build();
        assertTrue(MarathonbetSyncBatchSupport.needsSseRefresh(match, true, now, 48));
    }

    @Test
    void needsSseRefresh_falseWhenMatchAlreadyStarted() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("SCHEDULED")
                .utcKickoff(now.minus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(MarathonbetSyncBatchSupport.needsSseRefresh(match, false, now, 48));
    }

    @Test
    void needsSseRefresh_falseWhenFinalized() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .status("FINISHED")
                .utcKickoff(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        assertFalse(MarathonbetSyncBatchSupport.needsSseRefresh(match, false, Instant.now(), 48));
    }

    @Test
    void partitionStages_splitsIntoChunksOfFive() {
        List<MatchSchedule> matches = List.of(
                match("a", Instant.parse("2026-08-10T12:00:00Z")),
                match("b", Instant.parse("2026-08-10T14:00:00Z")),
                match("c", Instant.parse("2026-08-11T12:00:00Z")),
                match("d", Instant.parse("2026-08-11T14:00:00Z")),
                match("e", Instant.parse("2026-08-12T12:00:00Z")),
                match("f", Instant.parse("2026-08-12T14:00:00Z")),
                match("g", Instant.parse("2026-08-13T12:00:00Z")),
                match("h", Instant.parse("2026-08-13T14:00:00Z")),
                match("i", Instant.parse("2026-08-14T12:00:00Z")),
                match("j", Instant.parse("2026-08-14T14:00:00Z")),
                match("k", Instant.parse("2026-08-15T12:00:00Z")),
                match("l", Instant.parse("2026-08-15T14:00:00Z")),
                match("m", Instant.parse("2026-08-16T12:00:00Z")),
                match("n", Instant.parse("2026-08-16T14:00:00Z")),
                match("o", Instant.parse("2026-08-17T12:00:00Z")),
                match("p", Instant.parse("2026-08-17T14:00:00Z")),
                match("q", Instant.parse("2026-08-18T12:00:00Z")),
                match("r", Instant.parse("2026-08-18T14:00:00Z")),
                match("s", Instant.parse("2026-08-19T12:00:00Z")),
                match("t", Instant.parse("2026-08-19T14:00:00Z"))
        );
        List<MatchSchedule> sorted = MarathonbetSyncBatchSupport.sortByKickoff(matches);
        List<List<MatchSchedule>> stages = MarathonbetSyncBatchSupport.partitionStages(sorted, 5);
        assertEquals(4, stages.size());
        assertEquals(5, stages.get(0).size());
        assertEquals(5, stages.get(3).size());
        assertEquals("a", stages.get(0).get(0).getId());
        assertEquals("t", stages.get(3).get(4).getId());
    }

    @Test
    void sortByKickoff_ordersAscending() {
        List<MatchSchedule> sorted = MarathonbetSyncBatchSupport.sortByKickoff(List.of(
                match("late", Instant.parse("2026-08-20T18:00:00Z")),
                match("early", Instant.parse("2026-08-20T12:00:00Z"))
        ));
        assertEquals("early", sorted.get(0).getId());
        assertEquals("late", sorted.get(1).getId());
    }

    private static MatchSchedule match(String id, Instant kickoff) {
        return MatchSchedule.builder()
                .id(id)
                .status("SCHEDULED")
                .utcKickoff(kickoff)
                .build();
    }
}
