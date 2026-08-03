package net.friendly_bets.marathonbet;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pure helpers: stage batching by kickoff (provider-local).
 */
public final class MarathonbetSyncBatchSupport {

    private MarathonbetSyncBatchSupport() {
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
