package net.friendly_bets.oddsapi;

import net.friendly_bets.models.gameresults.GameResultRecord;

import java.time.LocalDateTime;
import java.util.Set;

public final class GameResultNotStarted {

    private static final Set<String> NOT_STARTED_STATUSES = Set.of("SCHEDULED", "TIMED");

    private GameResultNotStarted() {
    }

    public static boolean isNotStarted(GameResultRecord match, LocalDateTime now) {
        if (match == null) {
            return false;
        }
        String status = normalizeStatus(match.getStatus());
        if (status != null && !NOT_STARTED_STATUSES.contains(status)) {
            return false;
        }
        if (match.getUtcDate() != null && !match.getUtcDate().isAfter(now)) {
            return false;
        }
        return true;
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "PAUSE", "HALFTIME" -> "PAUSED";
            default -> status;
        };
    }
}
