package net.friendly_bets.footballdata;

import java.util.Set;

public final class FootballDataMatchStatuses {

    /** Матч завершён без дальнейших переходов (FINISHED, AWARDED, CANCELLED). */
    public static final Set<String> TERMINAL = Set.of(
            "FINISHED",
            "AWARDED",
            "CANCELLED"
    );

    /** Идёт игра (в т.ч. перерыв, доп. время, пенальти). */
    public static final Set<String> LIVE = Set.of(
            "IN_PLAY",
            "PAUSED",
            "EXTRA_TIME",
            "PENALTY_SHOOTOUT"
    );

    private FootballDataMatchStatuses() {
    }

    public static String normalize(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "PAUSE", "HALFTIME" -> "PAUSED";
            default -> status;
        };
    }

    public static boolean isTerminal(String status) {
        String normalized = normalize(status);
        return normalized != null && TERMINAL.contains(normalized);
    }

    public static boolean hasScore(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return false;
        }
        return isTerminal(normalized)
                || LIVE.contains(normalized)
                || "SUSPENDED".equals(normalized);
    }
}
