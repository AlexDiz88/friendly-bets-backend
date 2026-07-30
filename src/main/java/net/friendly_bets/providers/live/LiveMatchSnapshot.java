package net.friendly_bets.providers.live;

/**
 * Provider-agnostic LIVE row after mapping source-specific status/score/minute.
 * Written into {@code match_schedules} by {@link LiveMatchApplySupport}.
 */
public record LiveMatchSnapshot(
        String status,
        String rawMinuteLabel,
        String fullTimeScore,
        String firstTimeScore,
        String penaltyScore
) {
    public LiveMatchSnapshot {
        status = status == null ? null : status.trim();
        rawMinuteLabel = blankToNull(rawMinuteLabel);
        fullTimeScore = blankToNull(fullTimeScore);
        firstTimeScore = blankToNull(firstTimeScore);
        penaltyScore = blankToNull(penaltyScore);
    }

    /** Convenience for providers without penalty data. */
    public LiveMatchSnapshot(String status, String rawMinuteLabel, String fullTimeScore, String firstTimeScore) {
        this(status, rawMinuteLabel, fullTimeScore, firstTimeScore, null);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
