package net.friendly_bets.marathonbet;

import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.services.MatchScheduleDisplayService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pure helpers: SSE refresh policy and stage batching by kickoff.
 */
public final class MarathonbetSyncBatchSupport {

    private MarathonbetSyncBatchSupport() {
    }

    /**
     * Whether this not-started match should get an SSE fetch.
     * Missing odds → always; existing odds → only if kickoff within {@code refreshWithinHours}.
     */
    public static boolean needsSseRefresh(
            MatchSchedule match,
            boolean hasOdds,
            Instant now,
            int refreshWithinHours
    ) {
        if (match == null || match.getId() == null) {
            return false;
        }
        if (MatchScheduleDisplayService.isFinalized(match)) {
            return false;
        }
        if (!MatchScheduleNotStarted.isNotStarted(match, now)) {
            return false;
        }
        if (match.getUtcKickoff() == null) {
            return false;
        }
        if (!hasOdds) {
            return true;
        }
        Instant kickoff = match.getUtcKickoff();
        if (refreshWithinHours <= 0) {
            return true;
        }
        Duration untilKickoff = Duration.between(now, kickoff);
        return !untilKickoff.isNegative()
                && untilKickoff.compareTo(Duration.ofHours(refreshWithinHours)) <= 0;
    }

    public static List<MatchSchedule> sortByKickoff(List<MatchSchedule> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        return matches.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(MatchSchedule::getUtcKickoff, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MatchSchedule::getId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public static List<List<MatchSchedule>> partitionStages(List<MatchSchedule> sorted, int stageSize) {
        if (sorted == null || sorted.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, stageSize);
        List<List<MatchSchedule>> stages = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += size) {
            stages.add(List.copyOf(sorted.subList(i, Math.min(i + size, sorted.size()))));
        }
        return stages;
    }
}
