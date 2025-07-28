package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.BetTitleCode;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.MatchResult;
import net.friendly_bets.utils.BetCheckUtils.TotalType;

import static net.friendly_bets.utils.BetCheckUtils.MatchResult.*;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.OVER;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.UNDER;

public class GameResultWithTotalChecker implements BetChecker {

    @Override
    public BetStatus check(GameScore gameScore, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameScore);
        int home = gameScores.getHomeFullTime();
        int home1st = gameScores.getHomeFirstHalf();
        int home2nd = home - home1st;
        int away = gameScores.getAwayFullTime();
        int away1st = gameScores.getAwayFirstHalf();
        int away2nd = away - away1st;
        double total = home + away;
        double total1st = home1st + away1st;
        double total2nd = home2nd + away2nd;

        return switch (code) {
            case HOME_WIN_AND_UNDER_1_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 1.0);
            case HOME_WIN_AND_UNDER_1_5 -> evaluate(home, away, HOME_WIN, total, UNDER, 1.5);
            case HOME_WIN_AND_UNDER_2_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 2.0);
            case HOME_WIN_AND_UNDER_2_5 -> evaluate(home, away, HOME_WIN, total, UNDER, 2.5);
            case HOME_WIN_AND_UNDER_3_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 3.0);
            case HOME_WIN_AND_UNDER_3_5 -> evaluate(home, away, HOME_WIN, total, UNDER, 3.5);
            case HOME_WIN_AND_UNDER_4_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 4.0);
            case HOME_WIN_AND_UNDER_4_5 -> evaluate(home, away, HOME_WIN, total, UNDER, 4.5);
            case HOME_WIN_AND_UNDER_5_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 5.0);
            case HOME_WIN_AND_UNDER_5_5 -> evaluate(home, away, HOME_WIN, total, UNDER, 5.5);
            case HOME_WIN_AND_UNDER_6_0 -> evaluate(home, away, HOME_WIN, total, UNDER, 6.0);

            case HOME_WIN_AND_OVER_1_0 -> evaluate(home, away, HOME_WIN, total, OVER, 1.0);
            case HOME_WIN_AND_OVER_1_5 -> evaluate(home, away, HOME_WIN, total, OVER, 1.5);
            case HOME_WIN_AND_OVER_2_0 -> evaluate(home, away, HOME_WIN, total, OVER, 2.0);
            case HOME_WIN_AND_OVER_2_5 -> evaluate(home, away, HOME_WIN, total, OVER, 2.5);
            case HOME_WIN_AND_OVER_3_0 -> evaluate(home, away, HOME_WIN, total, OVER, 3.0);
            case HOME_WIN_AND_OVER_3_5 -> evaluate(home, away, HOME_WIN, total, OVER, 3.5);
            case HOME_WIN_AND_OVER_4_0 -> evaluate(home, away, HOME_WIN, total, OVER, 4.0);
            case HOME_WIN_AND_OVER_4_5 -> evaluate(home, away, HOME_WIN, total, OVER, 4.5);
            case HOME_WIN_AND_OVER_5_0 -> evaluate(home, away, HOME_WIN, total, OVER, 5.0);
            case HOME_WIN_AND_OVER_5_5 -> evaluate(home, away, HOME_WIN, total, OVER, 5.5);
            case HOME_WIN_AND_OVER_6_0 -> evaluate(home, away, HOME_WIN, total, OVER, 6.0);

            case HOME_OR_DRAW_AND_UNDER_1_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 1.0);
            case HOME_OR_DRAW_AND_UNDER_1_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 1.5);
            case HOME_OR_DRAW_AND_UNDER_2_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 2.0);
            case HOME_OR_DRAW_AND_UNDER_2_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 2.5);
            case HOME_OR_DRAW_AND_UNDER_3_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 3.0);
            case HOME_OR_DRAW_AND_UNDER_3_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 3.5);
            case HOME_OR_DRAW_AND_UNDER_4_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 4.0);
            case HOME_OR_DRAW_AND_UNDER_4_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 4.5);
            case HOME_OR_DRAW_AND_UNDER_5_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 5.0);
            case HOME_OR_DRAW_AND_UNDER_5_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 5.5);
            case HOME_OR_DRAW_AND_UNDER_6_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, UNDER, 6.0);

            case HOME_OR_DRAW_AND_OVER_1_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 1.0);
            case HOME_OR_DRAW_AND_OVER_1_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 1.5);
            case HOME_OR_DRAW_AND_OVER_2_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 2.0);
            case HOME_OR_DRAW_AND_OVER_2_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 2.5);
            case HOME_OR_DRAW_AND_OVER_3_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 3.0);
            case HOME_OR_DRAW_AND_OVER_3_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 3.5);
            case HOME_OR_DRAW_AND_OVER_4_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 4.0);
            case HOME_OR_DRAW_AND_OVER_4_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 4.5);
            case HOME_OR_DRAW_AND_OVER_5_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 5.0);
            case HOME_OR_DRAW_AND_OVER_5_5 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 5.5);
            case HOME_OR_DRAW_AND_OVER_6_0 -> evaluate(home, away, HOME_WIN_OR_DRAW, total, OVER, 6.0);

            case DRAW_AND_UNDER_1_0 -> evaluate(home, away, DRAW, total, UNDER, 1.0);
            case DRAW_AND_UNDER_1_5 -> evaluate(home, away, DRAW, total, UNDER, 1.5);
            case DRAW_AND_UNDER_2_0 -> evaluate(home, away, DRAW, total, UNDER, 2.0);
            case DRAW_AND_UNDER_2_5 -> evaluate(home, away, DRAW, total, UNDER, 2.5);
            case DRAW_AND_UNDER_3_0 -> evaluate(home, away, DRAW, total, UNDER, 3.0);
            case DRAW_AND_UNDER_3_5 -> evaluate(home, away, DRAW, total, UNDER, 3.5);
            case DRAW_AND_UNDER_4_0 -> evaluate(home, away, DRAW, total, UNDER, 4.0);
            case DRAW_AND_UNDER_4_5 -> evaluate(home, away, DRAW, total, UNDER, 4.5);
            case DRAW_AND_UNDER_5_0 -> evaluate(home, away, DRAW, total, UNDER, 5.0);
            case DRAW_AND_UNDER_5_5 -> evaluate(home, away, DRAW, total, UNDER, 5.5);
            case DRAW_AND_UNDER_6_0 -> evaluate(home, away, DRAW, total, UNDER, 6.0);

            case DRAW_AND_OVER_1_0 -> evaluate(home, away, DRAW, total, OVER, 1.0);
            case DRAW_AND_OVER_1_5 -> evaluate(home, away, DRAW, total, OVER, 1.5);
            case DRAW_AND_OVER_2_0 -> evaluate(home, away, DRAW, total, OVER, 2.0);
            case DRAW_AND_OVER_2_5 -> evaluate(home, away, DRAW, total, OVER, 2.5);
            case DRAW_AND_OVER_3_0 -> evaluate(home, away, DRAW, total, OVER, 3.0);
            case DRAW_AND_OVER_3_5 -> evaluate(home, away, DRAW, total, OVER, 3.5);
            case DRAW_AND_OVER_4_0 -> evaluate(home, away, DRAW, total, OVER, 4.0);
            case DRAW_AND_OVER_4_5 -> evaluate(home, away, DRAW, total, OVER, 4.5);
            case DRAW_AND_OVER_5_0 -> evaluate(home, away, DRAW, total, OVER, 5.0);
            case DRAW_AND_OVER_5_5 -> evaluate(home, away, DRAW, total, OVER, 5.5);
            case DRAW_AND_OVER_6_0 -> evaluate(home, away, DRAW, total, OVER, 6.0);

            case AWAY_WIN_AND_UNDER_1_5 -> evaluate(home, away, AWAY_WIN, total, UNDER, 1.5);
            case AWAY_WIN_AND_UNDER_2_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 2.0);
            case AWAY_WIN_AND_UNDER_2_5 -> evaluate(home, away, AWAY_WIN, total, UNDER, 2.5);
            case AWAY_WIN_AND_UNDER_1_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 1.0);
            case AWAY_WIN_AND_UNDER_3_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 3.0);
            case AWAY_WIN_AND_UNDER_3_5 -> evaluate(home, away, AWAY_WIN, total, UNDER, 3.5);
            case AWAY_WIN_AND_UNDER_4_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 4.0);
            case AWAY_WIN_AND_UNDER_4_5 -> evaluate(home, away, AWAY_WIN, total, UNDER, 4.5);
            case AWAY_WIN_AND_UNDER_5_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 5.0);
            case AWAY_WIN_AND_UNDER_5_5 -> evaluate(home, away, AWAY_WIN, total, UNDER, 5.5);
            case AWAY_WIN_AND_UNDER_6_0 -> evaluate(home, away, AWAY_WIN, total, UNDER, 6.0);

            case AWAY_WIN_AND_OVER_1_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 1.0);
            case AWAY_WIN_AND_OVER_1_5 -> evaluate(home, away, AWAY_WIN, total, OVER, 1.5);
            case AWAY_WIN_AND_OVER_2_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 2.0);
            case AWAY_WIN_AND_OVER_2_5 -> evaluate(home, away, AWAY_WIN, total, OVER, 2.5);
            case AWAY_WIN_AND_OVER_3_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 3.0);
            case AWAY_WIN_AND_OVER_3_5 -> evaluate(home, away, AWAY_WIN, total, OVER, 3.5);
            case AWAY_WIN_AND_OVER_4_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 4.0);
            case AWAY_WIN_AND_OVER_4_5 -> evaluate(home, away, AWAY_WIN, total, OVER, 4.5);
            case AWAY_WIN_AND_OVER_5_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 5.0);
            case AWAY_WIN_AND_OVER_5_5 -> evaluate(home, away, AWAY_WIN, total, OVER, 5.5);
            case AWAY_WIN_AND_OVER_6_0 -> evaluate(home, away, AWAY_WIN, total, OVER, 6.0);

            case AWAY_OR_DRAW_AND_UNDER_1_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 1.0);
            case AWAY_OR_DRAW_AND_UNDER_1_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 1.5);
            case AWAY_OR_DRAW_AND_UNDER_2_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 2.0);
            case AWAY_OR_DRAW_AND_UNDER_2_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 2.5);
            case AWAY_OR_DRAW_AND_UNDER_3_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 3.0);
            case AWAY_OR_DRAW_AND_UNDER_3_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 3.5);
            case AWAY_OR_DRAW_AND_UNDER_4_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 4.0);
            case AWAY_OR_DRAW_AND_UNDER_4_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 4.5);
            case AWAY_OR_DRAW_AND_UNDER_5_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 5.0);
            case AWAY_OR_DRAW_AND_UNDER_5_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 5.5);
            case AWAY_OR_DRAW_AND_UNDER_6_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, UNDER, 6.0);

            case AWAY_OR_DRAW_AND_OVER_1_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 1.0);
            case AWAY_OR_DRAW_AND_OVER_1_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 1.5);
            case AWAY_OR_DRAW_AND_OVER_2_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 2.0);
            case AWAY_OR_DRAW_AND_OVER_2_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 2.5);
            case AWAY_OR_DRAW_AND_OVER_3_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 3.0);
            case AWAY_OR_DRAW_AND_OVER_3_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 3.5);
            case AWAY_OR_DRAW_AND_OVER_4_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 4.0);
            case AWAY_OR_DRAW_AND_OVER_4_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 4.5);
            case AWAY_OR_DRAW_AND_OVER_5_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 5.0);
            case AWAY_OR_DRAW_AND_OVER_5_5 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 5.5);
            case AWAY_OR_DRAW_AND_OVER_6_0 -> evaluate(home, away, AWAY_WIN_OR_DRAW, total, OVER, 6.0);

            case HOME_OR_AWAY_AND_UNDER_1_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 1.0);
            case HOME_OR_AWAY_AND_UNDER_1_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 1.5);
            case HOME_OR_AWAY_AND_UNDER_2_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 2.0);
            case HOME_OR_AWAY_AND_UNDER_2_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 2.5);
            case HOME_OR_AWAY_AND_UNDER_3_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 3.0);
            case HOME_OR_AWAY_AND_UNDER_3_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 3.5);
            case HOME_OR_AWAY_AND_UNDER_4_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 4.0);
            case HOME_OR_AWAY_AND_UNDER_4_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 4.5);
            case HOME_OR_AWAY_AND_UNDER_5_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 5.0);
            case HOME_OR_AWAY_AND_UNDER_5_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 5.5);
            case HOME_OR_AWAY_AND_UNDER_6_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, UNDER, 6.0);

            case HOME_OR_AWAY_AND_OVER_1_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 1.0);
            case HOME_OR_AWAY_AND_OVER_1_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 1.5);
            case HOME_OR_AWAY_AND_OVER_2_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 2.0);
            case HOME_OR_AWAY_AND_OVER_2_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 2.5);
            case HOME_OR_AWAY_AND_OVER_3_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 3.0);
            case HOME_OR_AWAY_AND_OVER_3_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 3.5);
            case HOME_OR_AWAY_AND_OVER_4_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 4.0);
            case HOME_OR_AWAY_AND_OVER_4_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 4.5);
            case HOME_OR_AWAY_AND_OVER_5_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 5.0);
            case HOME_OR_AWAY_AND_OVER_5_5 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 5.5);
            case HOME_OR_AWAY_AND_OVER_6_0 -> evaluate(home, away, HOME_OR_AWAY_WIN, total, OVER, 6.0);

            // Исход + Тотал меньше (1й тайм)
            case FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, UNDER, 1.5);
            case FIRST_HALF_DRAW_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, DRAW, total1st, UNDER, 1.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, UNDER, 1.5);
            case FIRST_HALF_1X_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, UNDER, 1.5);
            case FIRST_HALF_12_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, UNDER, 1.5);
            case FIRST_HALF_X2_AND_TOTAL_UNDER_1_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, UNDER, 1.5);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, HOME_WIN, total1st, UNDER, 2.0);
            case FIRST_HALF_DRAW_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, DRAW, total1st, UNDER, 2.0);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, AWAY_WIN, total1st, UNDER, 2.0);
            case FIRST_HALF_1X_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, UNDER, 2.0);
            case FIRST_HALF_12_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, UNDER, 2.0);
            case FIRST_HALF_X2_AND_TOTAL_UNDER_2_0 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, UNDER, 2.0);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, UNDER, 2.5);
            case FIRST_HALF_DRAW_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, DRAW, total1st, UNDER, 2.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, UNDER, 2.5);
            case FIRST_HALF_1X_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, UNDER, 2.5);
            case FIRST_HALF_12_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, UNDER, 2.5);
            case FIRST_HALF_X2_AND_TOTAL_UNDER_2_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, UNDER, 2.5);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, HOME_WIN, total1st, UNDER, 3.0);
            case FIRST_HALF_DRAW_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, DRAW, total1st, UNDER, 3.0);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, AWAY_WIN, total1st, UNDER, 3.0);
            case FIRST_HALF_1X_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, UNDER, 3.0);
            case FIRST_HALF_12_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, UNDER, 3.0);
            case FIRST_HALF_X2_AND_TOTAL_UNDER_3_0 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, UNDER, 3.0);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, UNDER, 3.5);
            case FIRST_HALF_DRAW_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, DRAW, total1st, UNDER, 3.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, UNDER, 3.5);
            case FIRST_HALF_1X_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, UNDER, 3.5);
            case FIRST_HALF_12_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, UNDER, 3.5);
            case FIRST_HALF_X2_AND_TOTAL_UNDER_3_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, UNDER, 3.5);

            // Исход + Тотал больше (1й тайм)
            case FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, OVER, 1.5);
            case FIRST_HALF_DRAW_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, DRAW, total1st, OVER, 1.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, OVER, 1.5);
            case FIRST_HALF_1X_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, OVER, 1.5);
            case FIRST_HALF_12_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, OVER, 1.5);
            case FIRST_HALF_X2_AND_TOTAL_OVER_1_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, OVER, 1.5);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, HOME_WIN, total1st, OVER, 2.0);
            case FIRST_HALF_DRAW_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, DRAW, total1st, OVER, 2.0);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, AWAY_WIN, total1st, OVER, 2.0);
            case FIRST_HALF_1X_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, OVER, 2.0);
            case FIRST_HALF_12_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, OVER, 2.0);
            case FIRST_HALF_X2_AND_TOTAL_OVER_2_0 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, OVER, 2.0);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, OVER, 2.5);
            case FIRST_HALF_DRAW_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, DRAW, total1st, OVER, 2.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, OVER, 2.5);
            case FIRST_HALF_1X_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, OVER, 2.5);
            case FIRST_HALF_12_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, OVER, 2.5);
            case FIRST_HALF_X2_AND_TOTAL_OVER_2_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, OVER, 2.5);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, HOME_WIN, total1st, OVER, 3.0);
            case FIRST_HALF_DRAW_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, DRAW, total1st, OVER, 3.0);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, AWAY_WIN, total1st, OVER, 3.0);
            case FIRST_HALF_1X_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, OVER, 3.0);
            case FIRST_HALF_12_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, OVER, 3.0);
            case FIRST_HALF_X2_AND_TOTAL_OVER_3_0 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, OVER, 3.0);

            case FIRST_HALF_HOME_WIN_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, HOME_WIN, total1st, OVER, 3.5);
            case FIRST_HALF_DRAW_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, DRAW, total1st, OVER, 3.5);
            case FIRST_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, AWAY_WIN, total1st, OVER, 3.5);
            case FIRST_HALF_1X_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW, total1st, OVER, 3.5);
            case FIRST_HALF_12_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN, total1st, OVER, 3.5);
            case FIRST_HALF_X2_AND_TOTAL_OVER_3_5 -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW, total1st, OVER, 3.5);

            // Исход + Тотал меньше (2й тайм)
            case SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, UNDER, 1.5);
            case SECOND_HALF_DRAW_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, UNDER, 1.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, UNDER, 1.5);
            case SECOND_HALF_1X_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, UNDER, 1.5);
            case SECOND_HALF_12_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, UNDER, 1.5);
            case SECOND_HALF_X2_AND_TOTAL_UNDER_1_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, UNDER, 1.5);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, UNDER, 2.0);
            case SECOND_HALF_DRAW_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, DRAW, total2nd, UNDER, 2.0);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, UNDER, 2.0);
            case SECOND_HALF_1X_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, UNDER, 2.0);
            case SECOND_HALF_12_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, UNDER, 2.0);
            case SECOND_HALF_X2_AND_TOTAL_UNDER_2_0 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, UNDER, 2.0);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, UNDER, 2.5);
            case SECOND_HALF_DRAW_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, UNDER, 2.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, UNDER, 2.5);
            case SECOND_HALF_1X_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, UNDER, 2.5);
            case SECOND_HALF_12_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, UNDER, 2.5);
            case SECOND_HALF_X2_AND_TOTAL_UNDER_2_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, UNDER, 2.5);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, UNDER, 3.0);
            case SECOND_HALF_DRAW_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, DRAW, total2nd, UNDER, 3.0);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, UNDER, 3.0);
            case SECOND_HALF_1X_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, UNDER, 3.0);
            case SECOND_HALF_12_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, UNDER, 3.0);
            case SECOND_HALF_X2_AND_TOTAL_UNDER_3_0 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, UNDER, 3.0);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, UNDER, 3.5);
            case SECOND_HALF_DRAW_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, UNDER, 3.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, UNDER, 3.5);
            case SECOND_HALF_1X_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, UNDER, 3.5);
            case SECOND_HALF_12_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, UNDER, 3.5);
            case SECOND_HALF_X2_AND_TOTAL_UNDER_3_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, UNDER, 3.5);

            // Исход + Тотал больше (2й тайм)
            case SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, OVER, 1.5);
            case SECOND_HALF_DRAW_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, OVER, 1.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, OVER, 1.5);
            case SECOND_HALF_1X_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, OVER, 1.5);
            case SECOND_HALF_12_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, OVER, 1.5);
            case SECOND_HALF_X2_AND_TOTAL_OVER_1_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, OVER, 1.5);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, OVER, 2.0);
            case SECOND_HALF_DRAW_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, DRAW, total2nd, OVER, 2.0);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, OVER, 2.0);
            case SECOND_HALF_1X_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, OVER, 2.0);
            case SECOND_HALF_12_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, OVER, 2.0);
            case SECOND_HALF_X2_AND_TOTAL_OVER_2_0 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, OVER, 2.0);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, OVER, 2.5);
            case SECOND_HALF_DRAW_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, OVER, 2.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, OVER, 2.5);
            case SECOND_HALF_1X_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, OVER, 2.5);
            case SECOND_HALF_12_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, OVER, 2.5);
            case SECOND_HALF_X2_AND_TOTAL_OVER_2_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, OVER, 2.5);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, OVER, 3.0);
            case SECOND_HALF_DRAW_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, DRAW, total2nd, OVER, 3.0);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, OVER, 3.0);
            case SECOND_HALF_1X_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, OVER, 3.0);
            case SECOND_HALF_12_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, OVER, 3.0);
            case SECOND_HALF_X2_AND_TOTAL_OVER_3_0 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, OVER, 3.0);

            case SECOND_HALF_HOME_WIN_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, HOME_WIN, total2nd, OVER, 3.5);
            case SECOND_HALF_DRAW_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, DRAW, total2nd, OVER, 3.5);
            case SECOND_HALF_AWAY_WIN_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, AWAY_WIN, total2nd, OVER, 3.5);
            case SECOND_HALF_1X_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW, total2nd, OVER, 3.5);
            case SECOND_HALF_12_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN, total2nd, OVER, 3.5);
            case SECOND_HALF_X2_AND_TOTAL_OVER_3_5 -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW, total2nd, OVER, 3.5);

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(int home, int away, MatchResult expectedResult, double goalsTotal, TotalType type, double line) {
        boolean gameResult = BetCheckUtils.checkBetGameResult(home, away, expectedResult);
        if (!gameResult)
            return BetStatus.LOST;

        return switch (type) {
            case UNDER -> goalsTotal < line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
            case OVER -> goalsTotal > line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
        };
    }
}

