package net.friendly_bets.providers;

import net.friendly_bets.models.schedule.MatchSchedule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderMatchResolveSupportTest {

    private record Cand(String id, Instant kickoff, boolean sidesOk) {
    }

    @Test
    void uniqueMatchInWindow() {
        Instant kickoff = Instant.parse("2026-08-28T19:00:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(kickoff).build();
        List<Cand> candidates = List.of(
                new Cand("a", kickoff.minusSeconds(120), true),
                new Cand("b", kickoff.plusSeconds(7200), true)
        );
        var outcome = ProviderMatchResolveSupport.resolveUnique(
                schedule,
                candidates,
                Duration.ofMinutes(30),
                Cand::kickoff,
                Cand::sidesOk
        );
        assertTrue(outcome.isUnique());
        assertEquals("a", outcome.match().id());
    }

    @Test
    void missingWhenNoSides() {
        Instant kickoff = Instant.parse("2026-08-28T19:00:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(kickoff).build();
        List<Cand> candidates = List.of(new Cand("a", kickoff, false));
        var outcome = ProviderMatchResolveSupport.resolveUnique(
                schedule,
                candidates,
                Duration.ofMinutes(30),
                Cand::kickoff,
                Cand::sidesOk
        );
        assertTrue(outcome.isMissing());
        assertNull(outcome.match());
    }

    @Test
    void ambiguousWhenTwoCloseMatches() {
        Instant kickoff = Instant.parse("2026-08-28T19:00:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(kickoff).build();
        List<Cand> candidates = List.of(
                new Cand("a", kickoff.minusSeconds(60), true),
                new Cand("b", kickoff.plusSeconds(60), true)
        );
        var outcome = ProviderMatchResolveSupport.resolveUnique(
                schedule,
                candidates,
                Duration.ofMinutes(30),
                Cand::kickoff,
                Cand::sidesOk
        );
        assertTrue(outcome.isAmbiguous());
        assertNull(outcome.match());
    }

    @Test
    void preferringWindow_fallsBackToUniqueAliasOutsideWindow() {
        Instant scheduleKickoff = Instant.parse("2026-08-30T13:30:00Z");
        Instant bookieKickoff = Instant.parse("2026-08-29T13:30:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(scheduleKickoff).build();
        List<Cand> candidates = List.of(
                new Cand("freiburg", bookieKickoff, true),
                new Cand("other", bookieKickoff, false)
        );
        var inWindowOnly = ProviderMatchResolveSupport.resolveUnique(
                schedule, candidates, Duration.ofHours(12), Cand::kickoff, Cand::sidesOk);
        assertTrue(inWindowOnly.isMissing());

        var withFallback = ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                schedule, candidates, Duration.ofHours(12), Cand::kickoff, Cand::sidesOk);
        assertTrue(withFallback.isUnique());
        assertEquals("freiburg", withFallback.match().id());
    }

    @Test
    void preferringWindow_ambiguousWhenTwoAliasMatchesOutsideWindow() {
        Instant scheduleKickoff = Instant.parse("2026-08-30T13:30:00Z");
        Instant bookieKickoff = Instant.parse("2026-08-29T13:30:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(scheduleKickoff).build();
        List<Cand> candidates = List.of(
                new Cand("a", bookieKickoff, true),
                new Cand("b", bookieKickoff.plusSeconds(60), true)
        );
        var outcome = ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                schedule, candidates, Duration.ofHours(12), Cand::kickoff, Cand::sidesOk);
        assertTrue(outcome.isAmbiguous());
        assertNull(outcome.match());
    }

    @Test
    void preferringWindow_uniqueSidesWhenKickoffMissingOnFeed() {
        Instant scheduleKickoff = Instant.parse("2026-08-24T19:00:00Z");
        MatchSchedule schedule = MatchSchedule.builder().utcKickoff(scheduleKickoff).build();
        List<Cand> candidates = List.of(
                new Cand("fulham-chelsea", null, true),
                new Cand("other", scheduleKickoff, false)
        );
        var inWindowOnly = ProviderMatchResolveSupport.resolveUnique(
                schedule, candidates, Duration.ofHours(12), Cand::kickoff, Cand::sidesOk);
        assertTrue(inWindowOnly.isMissing());

        var withFallback = ProviderMatchResolveSupport.resolveUniquePreferringKickoffWindow(
                schedule, candidates, Duration.ofHours(12), Cand::kickoff, Cand::sidesOk);
        assertTrue(withFallback.isUnique());
        assertEquals("fulham-chelsea", withFallback.match().id());
    }
}
