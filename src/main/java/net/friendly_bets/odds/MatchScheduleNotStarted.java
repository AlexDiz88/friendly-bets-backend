package net.friendly_bets.odds;

import net.friendly_bets.models.schedule.MatchSchedule;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

public final class MatchScheduleNotStarted {

    private static final Set<String> NOT_STARTED_STATUSES = Set.of("SCHEDULED", "TIMED");

    private MatchScheduleNotStarted() {
    }

    public static boolean isNotStarted(MatchSchedule match) {
        return isNotStarted(match, Instant.now());
    }

    public static boolean isNotStarted(MatchSchedule match, Instant now) {
        if (match == null) {
            return false;
        }
        String status = normalizeStatus(match.getStatus());
        if (status != null && !NOT_STARTED_STATUSES.contains(status)) {
            return false;
        }
        Instant kickoff = match.getUtcKickoff();
        if (kickoff != null && !kickoff.isAfter(now)) {
            return false;
        }
        return true;
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PAUSE", "HALFTIME" -> "PAUSED";
            default -> status.trim().toUpperCase(Locale.ROOT);
        };
    }
}
