package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;

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
}
