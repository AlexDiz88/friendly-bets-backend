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
}
