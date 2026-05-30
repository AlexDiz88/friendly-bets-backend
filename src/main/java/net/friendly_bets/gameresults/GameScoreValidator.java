package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;

import java.util.Objects;
import java.util.regex.Pattern;

public final class GameScoreValidator {

    private static final Pattern SCORE_PART = Pattern.compile("^\\d{1,2}:\\d{1,2}$");

    private GameScoreValidator() {
    }

    public static boolean hasValidFullTime(GameScore score) {
        return score != null && isValidPart(score.getFullTime());
    }

    public static boolean hasValidFirstTime(GameScore score) {
        return score != null && isValidPart(score.getFirstTime());
    }

    public static boolean isValidPart(String part) {
        return part != null && !part.isBlank() && SCORE_PART.matcher(part.trim()).matches();
    }

    public static void requireValidCanonicalScore(GameScore score) {
        if (!hasValidFullTime(score)) {
            throw new net.friendly_bets.exceptions.BadRequestException("invalidGameScore");
        }
    }

    public static boolean sameCanonicalScore(GameScore left, GameScore right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(normalizePart(left.getFullTime()), normalizePart(right.getFullTime()))
                && Objects.equals(normalizePart(left.getFirstTime()), normalizePart(right.getFirstTime()))
                && Objects.equals(normalizePart(left.getOverTime()), normalizePart(right.getOverTime()))
                && Objects.equals(normalizePart(left.getPenalty()), normalizePart(right.getPenalty()));
    }

    public static String formatDisplay(GameScore score) {
        if (!hasValidFullTime(score)) {
            return "—";
        }
        String fullTime = score.getFullTime().trim();
        if (hasValidFirstTime(score)) {
            return fullTime + " (" + score.getFirstTime().trim() + ")";
        }
        return fullTime;
    }

    private static String normalizePart(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        return part.trim();
    }
}
