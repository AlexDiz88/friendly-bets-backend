package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;

import java.util.Objects;

/**
 * Сверка счёта между primary и secondary API.
 * Для обычных матчей — только {@code fullTime} и {@code firstTime}; OT/PEN — только при плей-офф.
 */
public final class ProviderScoreComparator {

    private ProviderScoreComparator() {
    }

    public static boolean matches(
            GameScore primary,
            GameScore secondary,
            String primaryDuration,
            String secondaryDuration
    ) {
        if (primary == null || secondary == null) {
            return false;
        }
        if (!GameScoreValidator.hasValidFullTime(primary) || !GameScoreValidator.hasValidFullTime(secondary)) {
            return false;
        }
        if (!partsEqual(primary.getFullTime(), secondary.getFullTime())) {
            return false;
        }
        if (!partsEqual(primary.getFirstTime(), secondary.getFirstTime())) {
            return false;
        }
        if (!isKnockoutDuration(primaryDuration) && !isKnockoutDuration(secondaryDuration)) {
            return true;
        }
        return partsEqual(primary.getOverTime(), secondary.getOverTime())
                && partsEqual(primary.getPenalty(), secondary.getPenalty());
    }

    private static boolean isKnockoutDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return false;
        }
        String normalized = duration.trim().toUpperCase();
        return CanonicalScoreNormalizer.DURATION_EXTRA_TIME.equals(normalized)
                || CanonicalScoreNormalizer.DURATION_PENALTY_SHOOTOUT.equals(normalized);
    }

    private static boolean partsEqual(String left, String right) {
        return Objects.equals(normalizePart(left), normalizePart(right));
    }

    private static String normalizePart(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        return part.trim();
    }
}
