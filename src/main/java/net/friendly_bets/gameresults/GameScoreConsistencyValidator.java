package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;

/**
 * Кросс-полевая проверка счёта (логика ставок / {@code BetUtils.checkGameScore}).
 */
public final class GameScoreConsistencyValidator {

    private GameScoreConsistencyValidator() {
    }

    public static boolean isConsistent(GameScore gameScore) {
        if (gameScore == null) {
            return false;
        }
        if (!GameScoreValidator.hasValidFullTime(gameScore) || !GameScoreValidator.hasValidFirstTime(gameScore)) {
            return false;
        }

        String fullTime = gameScore.getFullTime();
        String firstTime = gameScore.getFirstTime();
        String overTime = gameScore.getOverTime();
        String penalty = gameScore.getPenalty();

        int[] fullTimeScore = parseScorePart(fullTime);
        int[] firstTimeScore = parseScorePart(firstTime);

        if (fullTimeScore == null || firstTimeScore == null) {
            return false;
        }
        if (fullTimeScore[0] < firstTimeScore[0] || fullTimeScore[1] < firstTimeScore[1]) {
            return false;
        }

        if (overTime == null && penalty == null) {
            return true;
        }
        if (overTime != null) {
            int[] overTimeScore = parseScorePart(overTime);
            if (overTimeScore == null) {
                return false;
            }
            if (penalty == null) {
                return overTimeScore[0] != overTimeScore[1];
            }

            if (overTimeScore[0] != overTimeScore[1]) {
                return false;
            }

            int[] penaltyScore = parseScorePart(penalty);
            if (penaltyScore == null) {
                return false;
            }
            if (penaltyScore[0] == penaltyScore[1]) {
                return false;
            }

            int scoreDifference = Math.abs(penaltyScore[0] - penaltyScore[1]);
            return scoreDifference <= 3;
        }
        return false;
    }

    private static int[] parseScorePart(String score) {
        if (score == null || score.isEmpty()) {
            return null;
        }

        String[] parts = score.split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            int home = Integer.parseInt(parts[0]);
            int away = Integer.parseInt(parts[1]);

            if (home > 50 || away > 50
                    || (parts[0].length() > 1 && parts[0].startsWith("0"))
                    || (parts[1].length() > 1 && parts[1].startsWith("0"))) {
                return null;
            }

            return new int[]{home, away};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
