package net.friendly_bets.providers;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared FULL/ODDS-style resolve: kickoff window + predicate, ambiguous → empty with size &gt; 1 signal.
 * <p>
 * ODDS may also use {@link #resolveUniquePreferringKickoffWindow} when bookie provisional kickoff
 * diverges from {@code match_schedules.utc_kickoff} beyond the window.
 */
public final class ProviderMatchResolveSupport {

    private ProviderMatchResolveSupport() {
    }

    public record ResolveOutcome<T>(T match, int candidateCount) {
        public boolean isUnique() {
            return match != null && candidateCount == 1;
        }

        public boolean isAmbiguous() {
            return candidateCount > 1;
        }

        public boolean isMissing() {
            return candidateCount == 0;
        }
    }

    public static <T> ResolveOutcome<T> resolveUnique(
            MatchSchedule schedule,
            List<T> candidates,
            Duration kickoffWindow,
            Function<T, Instant> candidateKickoff,
            Predicate<T> sidesMatch
    ) {
        if (schedule == null || schedule.getUtcKickoff() == null || candidates == null || candidates.isEmpty()) {
            return new ResolveOutcome<>(null, 0);
        }
        Instant center = schedule.getUtcKickoff();
        long windowSeconds = Math.max(0L, kickoffWindow.getSeconds());
        List<T> inWindow = new ArrayList<>();
        for (T candidate : candidates) {
            Instant kickoff = candidateKickoff.apply(candidate);
            if (kickoff == null) {
                continue;
            }
            long delta = Math.abs(Duration.between(center, kickoff).getSeconds());
            if (delta <= windowSeconds && sidesMatch.test(candidate)) {
                inWindow.add(candidate);
            }
        }
        return disambiguateByKickoff(center, inWindow, candidateKickoff);
    }

    /**
     * ODDS resolve: (1) kickoff window + sides; (2) if none — sides across the full listing,
     * unique or closest-kickoff disambiguation. Schedule kickoff is never written by ODDS.
     */
    public static <T> ResolveOutcome<T> resolveUniquePreferringKickoffWindow(
            MatchSchedule schedule,
            List<T> candidates,
            Duration kickoffWindow,
            Function<T, Instant> candidateKickoff,
            Predicate<T> sidesMatch
    ) {
        ResolveOutcome<T> inWindow = resolveUnique(
                schedule, candidates, kickoffWindow, candidateKickoff, sidesMatch);
        if (!inWindow.isMissing()) {
            return inWindow;
        }
        if (schedule == null || schedule.getUtcKickoff() == null || candidates == null || candidates.isEmpty()) {
            return new ResolveOutcome<>(null, 0);
        }
        List<T> bySides = new ArrayList<>();
        for (T candidate : candidates) {
            if (sidesMatch.test(candidate)) {
                bySides.add(candidate);
            }
        }
        return disambiguateByKickoff(schedule.getUtcKickoff(), bySides, candidateKickoff);
    }

    /** Whether any candidate kickoff falls within the window (ignores sides). */
    public static <T> boolean anyKickoffInWindow(
            MatchSchedule schedule,
            List<T> candidates,
            Duration kickoffWindow,
            Function<T, Instant> candidateKickoff
    ) {
        if (schedule == null || schedule.getUtcKickoff() == null || candidates == null || candidates.isEmpty()) {
            return false;
        }
        Instant center = schedule.getUtcKickoff();
        long windowSeconds = Math.max(0L, kickoffWindow.getSeconds());
        for (T candidate : candidates) {
            Instant kickoff = candidateKickoff.apply(candidate);
            if (kickoff == null) {
                continue;
            }
            long delta = Math.abs(Duration.between(center, kickoff).getSeconds());
            if (delta <= windowSeconds) {
                return true;
            }
        }
        return false;
    }

    private static <T> ResolveOutcome<T> disambiguateByKickoff(
            Instant center,
            List<T> matched,
            Function<T, Instant> candidateKickoff
    ) {
        if (matched.isEmpty()) {
            return new ResolveOutcome<>(null, 0);
        }
        if (matched.size() == 1) {
            return new ResolveOutcome<>(matched.get(0), 1);
        }
        T closest = pickClosest(center, matched, candidateKickoff);
        if (closest == null) {
            return new ResolveOutcome<>(null, matched.size());
        }
        long bestDelta = Math.abs(Duration.between(
                center, Objects.requireNonNull(candidateKickoff.apply(closest))).getSeconds());
        for (T other : matched) {
            if (other == closest) {
                continue;
            }
            Instant otherKickoff = candidateKickoff.apply(other);
            if (otherKickoff == null) {
                continue;
            }
            long delta = Math.abs(Duration.between(center, otherKickoff).getSeconds());
            if (Math.abs(delta - bestDelta) < 3_600L) {
                return new ResolveOutcome<>(null, matched.size());
            }
        }
        return new ResolveOutcome<>(closest, 1);
    }

    private static <T> T pickClosest(Instant center, List<T> matched, Function<T, Instant> candidateKickoff) {
        T best = null;
        long bestDelta = Long.MAX_VALUE;
        for (T candidate : matched) {
            Instant kickoff = candidateKickoff.apply(candidate);
            if (kickoff == null) {
                continue;
            }
            long delta = Math.abs(Duration.between(center, kickoff).getSeconds());
            if (delta < bestDelta) {
                bestDelta = delta;
                best = candidate;
            }
        }
        return best;
    }
}
