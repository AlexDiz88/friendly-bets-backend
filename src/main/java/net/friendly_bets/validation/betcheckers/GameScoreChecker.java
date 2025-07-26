package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.BetTitleCode;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.utils.BetCheckUtils;

public class GameScoreChecker implements BetChecker {

    @Override
    public BetStatus check(GameResult gameResult, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameResult);
        double home = gameScores.getHomeFullTime();
        double home1st = gameScores.getHomeFirstHalf();
        double home2nd = home - home1st;
        double away = gameScores.getAwayFullTime();
        double away1st = gameScores.getAwayFirstHalf();
        double away2nd = away - away1st;

        return switch (code) {
            case GAME_SCORE_0_0 -> evaluate(home, away, 0, 0);
            case GAME_SCORE_1_0 -> evaluate(home, away, 1, 0);
            case GAME_SCORE_2_0 -> evaluate(home, away, 2, 0);
            case GAME_SCORE_3_0 -> evaluate(home, away, 3, 0);
            case GAME_SCORE_0_1 -> evaluate(home, away, 0, 1);
            case GAME_SCORE_1_1 -> evaluate(home, away, 1, 1);
            case GAME_SCORE_2_1 -> evaluate(home, away, 2, 1);
            case GAME_SCORE_3_1 -> evaluate(home, away, 3, 1);
            case GAME_SCORE_0_2 -> evaluate(home, away, 0, 2);
            case GAME_SCORE_1_2 -> evaluate(home, away, 1, 2);
            case GAME_SCORE_2_2 -> evaluate(home, away, 2, 2);
            case GAME_SCORE_3_2 -> evaluate(home, away, 3, 2);
            case GAME_SCORE_0_3 -> evaluate(home, away, 0, 3);
            case GAME_SCORE_1_3 -> evaluate(home, away, 1, 3);
            case GAME_SCORE_2_3 -> evaluate(home, away, 2, 3);
            case GAME_SCORE_3_3 -> evaluate(home, away, 3, 3);

            case GAME_SCORE_0_4 -> evaluate(home, away, 0, 4);
            case GAME_SCORE_1_4 -> evaluate(home, away, 1, 4);
            case GAME_SCORE_2_4 -> evaluate(home, away, 2, 4);
            case GAME_SCORE_3_4 -> evaluate(home, away, 3, 4);
            case GAME_SCORE_4_0 -> evaluate(home, away, 4, 0);
            case GAME_SCORE_4_1 -> evaluate(home, away, 4, 1);
            case GAME_SCORE_4_2 -> evaluate(home, away, 4, 2);
            case GAME_SCORE_4_3 -> evaluate(home, away, 4, 3);
            case GAME_SCORE_4_4 -> evaluate(home, away, 4, 4);
            case GAME_SCORE_0_5 -> evaluate(home, away, 0, 5);
            case GAME_SCORE_1_5 -> evaluate(home, away, 1, 5);
            case GAME_SCORE_2_5 -> evaluate(home, away, 2, 5);
            case GAME_SCORE_3_5 -> evaluate(home, away, 3, 5);
            case GAME_SCORE_4_5 -> evaluate(home, away, 4, 5);
            case GAME_SCORE_5_0 -> evaluate(home, away, 5, 0);
            case GAME_SCORE_5_1 -> evaluate(home, away, 5, 1);
            case GAME_SCORE_5_2 -> evaluate(home, away, 5, 2);
            case GAME_SCORE_5_3 -> evaluate(home, away, 5, 3);
            case GAME_SCORE_5_4 -> evaluate(home, away, 5, 4);
            case GAME_SCORE_5_5 -> evaluate(home, away, 5, 5);
            case GAME_SCORE_0_6 -> evaluate(home, away, 0, 6);
            case GAME_SCORE_1_6 -> evaluate(home, away, 1, 6);
            case GAME_SCORE_2_6 -> evaluate(home, away, 2, 6);
            case GAME_SCORE_3_6 -> evaluate(home, away, 3, 6);
            case GAME_SCORE_4_6 -> evaluate(home, away, 4, 6);
            case GAME_SCORE_5_6 -> evaluate(home, away, 5, 6);
            case GAME_SCORE_6_0 -> evaluate(home, away, 6, 0);
            case GAME_SCORE_6_1 -> evaluate(home, away, 6, 1);
            case GAME_SCORE_6_2 -> evaluate(home, away, 6, 2);
            case GAME_SCORE_6_3 -> evaluate(home, away, 6, 3);
            case GAME_SCORE_6_4 -> evaluate(home, away, 6, 4);
            case GAME_SCORE_6_5 -> evaluate(home, away, 6, 5);
            case GAME_SCORE_6_6 -> evaluate(home, away, 6, 6);
            case GAME_SCORE_0_7 -> evaluate(home, away, 0, 7);
            case GAME_SCORE_1_7 -> evaluate(home, away, 1, 7);
            case GAME_SCORE_2_7 -> evaluate(home, away, 2, 7);
            case GAME_SCORE_3_7 -> evaluate(home, away, 3, 7);
            case GAME_SCORE_4_7 -> evaluate(home, away, 4, 7);
            case GAME_SCORE_5_7 -> evaluate(home, away, 5, 7);
            case GAME_SCORE_6_7 -> evaluate(home, away, 6, 7);
            case GAME_SCORE_7_0 -> evaluate(home, away, 7, 0);
            case GAME_SCORE_7_1 -> evaluate(home, away, 7, 1);
            case GAME_SCORE_7_2 -> evaluate(home, away, 7, 2);
            case GAME_SCORE_7_3 -> evaluate(home, away, 7, 3);
            case GAME_SCORE_7_4 -> evaluate(home, away, 7, 4);
            case GAME_SCORE_7_5 -> evaluate(home, away, 7, 5);
            case GAME_SCORE_7_6 -> evaluate(home, away, 7, 6);
            case GAME_SCORE_7_7 -> evaluate(home, away, 7, 7);

            case FIRST_HALF_SCORE_0_0 -> evaluate(home1st, away1st, 0, 0);
            case FIRST_HALF_SCORE_1_0 -> evaluate(home1st, away1st, 1, 0);
            case FIRST_HALF_SCORE_2_0 -> evaluate(home1st, away1st, 2, 0);
            case FIRST_HALF_SCORE_3_0 -> evaluate(home1st, away1st, 3, 0);
            case FIRST_HALF_SCORE_0_1 -> evaluate(home1st, away1st, 0, 1);
            case FIRST_HALF_SCORE_1_1 -> evaluate(home1st, away1st, 1, 1);
            case FIRST_HALF_SCORE_2_1 -> evaluate(home1st, away1st, 2, 1);
            case FIRST_HALF_SCORE_3_1 -> evaluate(home1st, away1st, 3, 1);
            case FIRST_HALF_SCORE_0_2 -> evaluate(home1st, away1st, 0, 2);
            case FIRST_HALF_SCORE_1_2 -> evaluate(home1st, away1st, 1, 2);
            case FIRST_HALF_SCORE_2_2 -> evaluate(home1st, away1st, 2, 2);
            case FIRST_HALF_SCORE_3_2 -> evaluate(home1st, away1st, 3, 2);
            case FIRST_HALF_SCORE_0_3 -> evaluate(home1st, away1st, 0, 3);
            case FIRST_HALF_SCORE_1_3 -> evaluate(home1st, away1st, 1, 3);
            case FIRST_HALF_SCORE_2_3 -> evaluate(home1st, away1st, 2, 3);
            case FIRST_HALF_SCORE_3_3 -> evaluate(home1st, away1st, 3, 3);

            case SECOND_HALF_SCORE_0_0 -> evaluate(home2nd, away2nd, 0, 0);
            case SECOND_HALF_SCORE_1_0 -> evaluate(home2nd, away2nd, 1, 0);
            case SECOND_HALF_SCORE_2_0 -> evaluate(home2nd, away2nd, 2, 0);
            case SECOND_HALF_SCORE_3_0 -> evaluate(home2nd, away2nd, 3, 0);
            case SECOND_HALF_SCORE_0_1 -> evaluate(home2nd, away2nd, 0, 1);
            case SECOND_HALF_SCORE_1_1 -> evaluate(home2nd, away2nd, 1, 1);
            case SECOND_HALF_SCORE_2_1 -> evaluate(home2nd, away2nd, 2, 1);
            case SECOND_HALF_SCORE_3_1 -> evaluate(home2nd, away2nd, 3, 1);
            case SECOND_HALF_SCORE_0_2 -> evaluate(home2nd, away2nd, 0, 2);
            case SECOND_HALF_SCORE_1_2 -> evaluate(home2nd, away2nd, 1, 2);
            case SECOND_HALF_SCORE_2_2 -> evaluate(home2nd, away2nd, 2, 2);
            case SECOND_HALF_SCORE_3_2 -> evaluate(home2nd, away2nd, 3, 2);
            case SECOND_HALF_SCORE_0_3 -> evaluate(home2nd, away2nd, 0, 3);
            case SECOND_HALF_SCORE_1_3 -> evaluate(home2nd, away2nd, 1, 3);
            case SECOND_HALF_SCORE_2_3 -> evaluate(home2nd, away2nd, 2, 3);
            case SECOND_HALF_SCORE_3_3 -> evaluate(home2nd, away2nd, 3, 3);

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(double home, double away, double expectedHome, double expectedAway) {
        return home == expectedHome && away == expectedAway ? BetStatus.WON : BetStatus.LOST;
    }
}
