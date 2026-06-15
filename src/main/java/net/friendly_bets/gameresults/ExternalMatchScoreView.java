package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;

import java.util.Set;

/** Mirrors frontend {@code externalMatchScoreView.ts}. */
public final class ExternalMatchScoreView {

    private static final String SCORE_UNAVAILABLE = "—";

    private static final Set<String> LIVE_STATUSES = Set.of(
            "IN_PLAY",
            "PAUSED",
            "EXTRA_TIME",
            "PENALTY_SHOOTOUT"
    );

    private ExternalMatchScoreView() {
    }

    public static String format(GameScore gameScore, String matchStatus, boolean finalized) {
        return format(gameScore, matchStatus, finalized, null);
    }

    public static String format(
            GameScore gameScore,
            String matchStatus,
            boolean finalized,
            String liveMinuteLabel
    ) {
        String status = normalizeStatus(matchStatus);
        boolean trustLive = LIVE_STATUSES.contains(status)
                && !finalized
                && (hasFullTime(gameScore) || hasText(liveMinuteLabel));
        if (LIVE_STATUSES.contains(status) && !finalized && !trustLive) {
            return SCORE_UNAVAILABLE;
        }
        if (!hasFullTime(gameScore)) {
            return SCORE_UNAVAILABLE;
        }
        if (trustLive) {
            return gameScore.getFullTime();
        }
        if (gameScore.getFirstTime() != null && !gameScore.getFirstTime().isBlank()) {
            return gameScore.getFullTime() + " (" + gameScore.getFirstTime() + ")";
        }
        return gameScore.getFullTime();
    }

    private static boolean hasFullTime(GameScore gameScore) {
        return gameScore != null
                && gameScore.getFullTime() != null
                && !gameScore.getFullTime().isBlank();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return status.trim().toUpperCase().replace(' ', '_');
    }
}
