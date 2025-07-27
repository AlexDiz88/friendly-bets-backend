package net.friendly_bets.utils;

import lombok.experimental.UtilityClass;
import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.GameResult;

@UtilityClass
public class BetCheckUtils {

    public enum CompareSign {
        LESS, EQUAL, MORE
    }

    public enum TotalType {
        UNDER, OVER
    }

    public enum HandicapType {
        PLUS, MINUS
    }

    public enum MatchResult {
        HOME_WIN, DRAW, AWAY_WIN, HOME_WIN_OR_DRAW, AWAY_WIN_OR_DRAW, HOME_OR_AWAY_WIN
    }

    public GameScores parse(GameResult result) {
        return GameScores.builder()
                .homeFullTime(parseGoals(result.getFullTime(), 0))
                .awayFullTime(parseGoals(result.getFullTime(), 1))
                .homeFirstHalf(parseGoals(result.getFirstTime(), 0))
                .awayFirstHalf(parseGoals(result.getFirstTime(), 1))
                .homeOverTime(parseGoals(result.getOverTime(), 0))
                .awayOverTime(parseGoals(result.getOverTime(), 1))
                .homePenalty(parseGoals(result.getPenalty(), 0))
                .awayPenalty(parseGoals(result.getPenalty(), 1))
                .build();
    }

    /**
     * Returns -1 if score is null, improperly formatted or non-numeric.
     */
    private int parseGoals(String score, int index) {
        if (score == null || !score.contains(":")) return -1;
        String[] parts = score.split(":");
        if (parts.length != 2) return 0;
        try {
            return Integer.parseInt(parts[index].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean checkBetGameResult(int home, int away, MatchResult expectedResult) {
        boolean homeWin = home > away;
        boolean draw = home == away;
        boolean awayWin = home < away;

        return switch (expectedResult) {
            case HOME_WIN -> homeWin;
            case DRAW -> draw;
            case AWAY_WIN -> awayWin;
            case HOME_WIN_OR_DRAW -> homeWin || draw;
            case AWAY_WIN_OR_DRAW -> awayWin || draw;
            case HOME_OR_AWAY_WIN -> homeWin || awayWin;
        };
    }
}
