package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.CompareSign;
import net.friendly_bets.utils.BetCheckUtils.TotalType;

import static net.friendly_bets.utils.BetCheckUtils.CompareSign.*;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.OVER;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.UNDER;

public class TotalChecker implements BetChecker {

    @Override
    public BetStatus check(GameScore gameScore, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameScore);
        double home = gameScores.getHomeFullTime();
        double home1st = gameScores.getHomeFirstHalf();
        double home2nd = home - home1st;
        double away = gameScores.getAwayFullTime();
        double away1st = gameScores.getAwayFirstHalf();
        double away2nd = away - away1st;
        double total = home + away;
        double total1st = home1st + away1st;
        double total2nd = home2nd + away2nd;

        return switch (code) {
            // Общий Тотал
            case TOTAL_UNDER_1_0 -> evaluate(total, UNDER, 1.0);
            case TOTAL_UNDER_1_5 -> evaluate(total, UNDER, 1.5);
            case TOTAL_UNDER_2_0 -> evaluate(total, UNDER, 2.0);
            case TOTAL_UNDER_2_5 -> evaluate(total, UNDER, 2.5);
            case TOTAL_UNDER_3_0 -> evaluate(total, UNDER, 3.0);
            case TOTAL_UNDER_3_5 -> evaluate(total, UNDER, 3.5);
            case TOTAL_UNDER_4_0 -> evaluate(total, UNDER, 4.0);
            case TOTAL_UNDER_4_5 -> evaluate(total, UNDER, 4.5);
            case TOTAL_UNDER_5_0 -> evaluate(total, UNDER, 5.0);
            case TOTAL_UNDER_5_5 -> evaluate(total, UNDER, 5.5);
            case TOTAL_UNDER_6_0 -> evaluate(total, UNDER, 6.0);
            case TOTAL_UNDER_6_5 -> evaluate(total, UNDER, 6.5);

            case TOTAL_OVER_1_0 -> evaluate(total, OVER, 1.0);
            case TOTAL_OVER_1_5 -> evaluate(total, OVER, 1.5);
            case TOTAL_OVER_2_0 -> evaluate(total, OVER, 2.0);
            case TOTAL_OVER_2_5 -> evaluate(total, OVER, 2.5);
            case TOTAL_OVER_3_0 -> evaluate(total, OVER, 3.0);
            case TOTAL_OVER_3_5 -> evaluate(total, OVER, 3.5);
            case TOTAL_OVER_4_0 -> evaluate(total, OVER, 4.0);
            case TOTAL_OVER_4_5 -> evaluate(total, OVER, 4.5);
            case TOTAL_OVER_5_0 -> evaluate(total, OVER, 5.0);
            case TOTAL_OVER_5_5 -> evaluate(total, OVER, 5.5);
            case TOTAL_OVER_6_0 -> evaluate(total, OVER, 6.0);
            case TOTAL_OVER_6_5 -> evaluate(total, OVER, 6.5);

            // Индивидуальный Тотал
            case HOME_TEAM_UNDER_1_0 -> evaluate(home, UNDER, 1.0);
            case HOME_TEAM_UNDER_1_5 -> evaluate(home, UNDER, 1.5);
            case HOME_TEAM_UNDER_2_0 -> evaluate(home, UNDER, 2.0);
            case HOME_TEAM_UNDER_2_5 -> evaluate(home, UNDER, 2.5);
            case HOME_TEAM_UNDER_3_0 -> evaluate(home, UNDER, 3.0);
            case HOME_TEAM_UNDER_3_5 -> evaluate(home, UNDER, 3.5);
            case HOME_TEAM_UNDER_4_0 -> evaluate(home, UNDER, 4.0);
            case HOME_TEAM_UNDER_4_5 -> evaluate(home, UNDER, 4.5);
            case HOME_TEAM_UNDER_5_0 -> evaluate(home, UNDER, 5.0);
            case HOME_TEAM_UNDER_5_5 -> evaluate(home, UNDER, 5.5);
            case HOME_TEAM_UNDER_6_0 -> evaluate(home, UNDER, 6.0);
            case HOME_TEAM_UNDER_6_5 -> evaluate(home, UNDER, 6.5);

            case HOME_TEAM_OVER_1_0 -> evaluate(home, OVER, 1.0);
            case HOME_TEAM_OVER_1_5 -> evaluate(home, OVER, 1.5);
            case HOME_TEAM_OVER_2_0 -> evaluate(home, OVER, 2.0);
            case HOME_TEAM_OVER_2_5 -> evaluate(home, OVER, 2.5);
            case HOME_TEAM_OVER_3_0 -> evaluate(home, OVER, 3.0);
            case HOME_TEAM_OVER_3_5 -> evaluate(home, OVER, 3.5);
            case HOME_TEAM_OVER_4_0 -> evaluate(home, OVER, 4.0);
            case HOME_TEAM_OVER_4_5 -> evaluate(home, OVER, 4.5);
            case HOME_TEAM_OVER_5_0 -> evaluate(home, OVER, 5.0);
            case HOME_TEAM_OVER_5_5 -> evaluate(home, OVER, 5.5);
            case HOME_TEAM_OVER_6_0 -> evaluate(home, OVER, 6.0);
            case HOME_TEAM_OVER_6_5 -> evaluate(home, OVER, 6.5);

            case AWAY_TEAM_UNDER_1_0 -> evaluate(away, UNDER, 1.0);
            case AWAY_TEAM_UNDER_1_5 -> evaluate(away, UNDER, 1.5);
            case AWAY_TEAM_UNDER_2_0 -> evaluate(away, UNDER, 2.0);
            case AWAY_TEAM_UNDER_2_5 -> evaluate(away, UNDER, 2.5);
            case AWAY_TEAM_UNDER_3_0 -> evaluate(away, UNDER, 3.0);
            case AWAY_TEAM_UNDER_3_5 -> evaluate(away, UNDER, 3.5);
            case AWAY_TEAM_UNDER_4_0 -> evaluate(away, UNDER, 4.0);
            case AWAY_TEAM_UNDER_4_5 -> evaluate(away, UNDER, 4.5);
            case AWAY_TEAM_UNDER_5_0 -> evaluate(away, UNDER, 5.0);
            case AWAY_TEAM_UNDER_5_5 -> evaluate(away, UNDER, 5.5);
            case AWAY_TEAM_UNDER_6_0 -> evaluate(away, UNDER, 6.0);
            case AWAY_TEAM_UNDER_6_5 -> evaluate(away, UNDER, 6.5);

            case AWAY_TEAM_OVER_1_0 -> evaluate(away, OVER, 1.0);
            case AWAY_TEAM_OVER_1_5 -> evaluate(away, OVER, 1.5);
            case AWAY_TEAM_OVER_2_0 -> evaluate(away, OVER, 2.0);
            case AWAY_TEAM_OVER_2_5 -> evaluate(away, OVER, 2.5);
            case AWAY_TEAM_OVER_3_0 -> evaluate(away, OVER, 3.0);
            case AWAY_TEAM_OVER_3_5 -> evaluate(away, OVER, 3.5);
            case AWAY_TEAM_OVER_4_0 -> evaluate(away, OVER, 4.0);
            case AWAY_TEAM_OVER_4_5 -> evaluate(away, OVER, 4.5);
            case AWAY_TEAM_OVER_5_0 -> evaluate(away, OVER, 5.0);
            case AWAY_TEAM_OVER_5_5 -> evaluate(away, OVER, 5.5);
            case AWAY_TEAM_OVER_6_0 -> evaluate(away, OVER, 6.0);
            case AWAY_TEAM_OVER_6_5 -> evaluate(away, OVER, 6.5);

            // Общий Тотал по таймам
            case FIRST_HALF_TOTAL_UNDER_0_5 -> evaluate(total1st, UNDER, 0.5);
            case FIRST_HALF_TOTAL_UNDER_1_0 -> evaluate(total1st, UNDER, 1.0);
            case FIRST_HALF_TOTAL_UNDER_1_5 -> evaluate(total1st, UNDER, 1.5);
            case FIRST_HALF_TOTAL_UNDER_2_0 -> evaluate(total1st, UNDER, 2.0);
            case FIRST_HALF_TOTAL_UNDER_2_5 -> evaluate(total1st, UNDER, 2.5);
            case FIRST_HALF_TOTAL_UNDER_3_0 -> evaluate(total1st, UNDER, 3.0);
            case FIRST_HALF_TOTAL_UNDER_3_5 -> evaluate(total1st, UNDER, 3.5);
            case FIRST_HALF_TOTAL_UNDER_4_0 -> evaluate(total1st, UNDER, 4.0);
            case FIRST_HALF_TOTAL_UNDER_4_5 -> evaluate(total1st, UNDER, 4.5);

            case FIRST_HALF_TOTAL_OVER_0_5 -> evaluate(total1st, OVER, 0.5);
            case FIRST_HALF_TOTAL_OVER_1_0 -> evaluate(total1st, OVER, 1.0);
            case FIRST_HALF_TOTAL_OVER_1_5 -> evaluate(total1st, OVER, 1.5);
            case FIRST_HALF_TOTAL_OVER_2_0 -> evaluate(total1st, OVER, 2.0);
            case FIRST_HALF_TOTAL_OVER_2_5 -> evaluate(total1st, OVER, 2.5);
            case FIRST_HALF_TOTAL_OVER_3_0 -> evaluate(total1st, OVER, 3.0);
            case FIRST_HALF_TOTAL_OVER_3_5 -> evaluate(total1st, OVER, 3.5);
            case FIRST_HALF_TOTAL_OVER_4_0 -> evaluate(total1st, OVER, 4.0);
            case FIRST_HALF_TOTAL_OVER_4_5 -> evaluate(total1st, OVER, 4.5);

            case SECOND_HALF_TOTAL_UNDER_0_5 -> evaluate(total2nd, UNDER, 0.5);
            case SECOND_HALF_TOTAL_UNDER_1_0 -> evaluate(total2nd, UNDER, 1.0);
            case SECOND_HALF_TOTAL_UNDER_1_5 -> evaluate(total2nd, UNDER, 1.5);
            case SECOND_HALF_TOTAL_UNDER_2_0 -> evaluate(total2nd, UNDER, 2.0);
            case SECOND_HALF_TOTAL_UNDER_2_5 -> evaluate(total2nd, UNDER, 2.5);
            case SECOND_HALF_TOTAL_UNDER_3_0 -> evaluate(total2nd, UNDER, 3.0);
            case SECOND_HALF_TOTAL_UNDER_3_5 -> evaluate(total2nd, UNDER, 3.5);
            case SECOND_HALF_TOTAL_UNDER_4_0 -> evaluate(total2nd, UNDER, 4.0);
            case SECOND_HALF_TOTAL_UNDER_4_5 -> evaluate(total2nd, UNDER, 4.5);

            case SECOND_HALF_TOTAL_OVER_0_5 -> evaluate(total2nd, OVER, 0.5);
            case SECOND_HALF_TOTAL_OVER_1_0 -> evaluate(total2nd, OVER, 1.0);
            case SECOND_HALF_TOTAL_OVER_1_5 -> evaluate(total2nd, OVER, 1.5);
            case SECOND_HALF_TOTAL_OVER_2_0 -> evaluate(total2nd, OVER, 2.0);
            case SECOND_HALF_TOTAL_OVER_2_5 -> evaluate(total2nd, OVER, 2.5);
            case SECOND_HALF_TOTAL_OVER_3_0 -> evaluate(total2nd, OVER, 3.0);
            case SECOND_HALF_TOTAL_OVER_3_5 -> evaluate(total2nd, OVER, 3.5);
            case SECOND_HALF_TOTAL_OVER_4_0 -> evaluate(total2nd, OVER, 4.0);
            case SECOND_HALF_TOTAL_OVER_4_5 -> evaluate(total2nd, OVER, 4.5);

            // Индивидуальный Тотал по таймам (Хозяева)
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_0_5 -> evaluate(home1st, UNDER, 0.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1_0 -> evaluate(home1st, UNDER, 1.0);
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_1_5 -> evaluate(home1st, UNDER, 1.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2_0 -> evaluate(home1st, UNDER, 2.0);
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_2_5 -> evaluate(home1st, UNDER, 2.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_UNDER_3_0 -> evaluate(home1st, UNDER, 3.0);

            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_0_5 -> evaluate(home1st, OVER, 0.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_1_0 -> evaluate(home1st, OVER, 1.0);
            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_1_5 -> evaluate(home1st, OVER, 1.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_2_0 -> evaluate(home1st, OVER, 2.0);
            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_2_5 -> evaluate(home1st, OVER, 2.5);
            case FIRST_HALF_HOME_TEAM_TOTAL_OVER_3_0 -> evaluate(home1st, OVER, 3.0);

            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_0_5 -> evaluate(home2nd, UNDER, 0.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1_0 -> evaluate(home2nd, UNDER, 1.0);
            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_1_5 -> evaluate(home2nd, UNDER, 1.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2_0 -> evaluate(home2nd, UNDER, 2.0);
            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_2_5 -> evaluate(home2nd, UNDER, 2.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_UNDER_3_0 -> evaluate(home2nd, UNDER, 3.0);

            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_0_5 -> evaluate(home2nd, OVER, 0.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_1_0 -> evaluate(home2nd, OVER, 1.0);
            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_1_5 -> evaluate(home2nd, OVER, 1.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_2_0 -> evaluate(home2nd, OVER, 2.0);
            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_2_5 -> evaluate(home2nd, OVER, 2.5);
            case SECOND_HALF_HOME_TEAM_TOTAL_OVER_3_0 -> evaluate(home2nd, OVER, 3.0);

            // Индивидуальный Тотал по таймам (Гости)
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_0_5 -> evaluate(away1st, UNDER, 0.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1_0 -> evaluate(away1st, UNDER, 1.0);
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_1_5 -> evaluate(away1st, UNDER, 1.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2_0 -> evaluate(away1st, UNDER, 2.0);
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_2_5 -> evaluate(away1st, UNDER, 2.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_UNDER_3_0 -> evaluate(away1st, UNDER, 3.0);

            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_0_5 -> evaluate(away1st, OVER, 0.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1_0 -> evaluate(away1st, OVER, 1.0);
            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_1_5 -> evaluate(away1st, OVER, 1.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2_0 -> evaluate(away1st, OVER, 2.0);
            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_2_5 -> evaluate(away1st, OVER, 2.5);
            case FIRST_HALF_AWAY_TEAM_TOTAL_OVER_3_0 -> evaluate(away1st, OVER, 3.0);

            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_0_5 -> evaluate(away2nd, UNDER, 0.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1_0 -> evaluate(away2nd, UNDER, 1.0);
            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_1_5 -> evaluate(away2nd, UNDER, 1.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2_0 -> evaluate(away2nd, UNDER, 2.0);
            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_2_5 -> evaluate(away2nd, UNDER, 2.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_UNDER_3_0 -> evaluate(away2nd, UNDER, 3.0);

            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_0_5 -> evaluate(away2nd, OVER, 0.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1_0 -> evaluate(away2nd, OVER, 1.0);
            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_1_5 -> evaluate(away2nd, OVER, 1.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2_0 -> evaluate(away2nd, OVER, 2.0);
            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_2_5 -> evaluate(away2nd, OVER, 2.5);
            case SECOND_HALF_AWAY_TEAM_TOTAL_OVER_3_0 -> evaluate(away2nd, OVER, 3.0);

            // Тайм с большим количеством голов
            case TOTAL_GOALS_FIRST_HALF_MORE_THAN_SECOND -> evaluate(total1st, MORE, total2nd);
            case TOTAL_GOALS_EQUAL_IN_BOTH_HALVES -> evaluate(total1st, EQUAL, total2nd);
            case TOTAL_GOALS_FIRST_HALF_LESS_THAN_SECOND -> evaluate(total1st, LESS, total2nd);
            case HOME_GOALS_FIRST_HALF_MORE_THAN_SECOND -> evaluate(home1st, MORE, home2nd);
            case HOME_GOALS_EQUAL_IN_BOTH_HALVES -> evaluate(home1st, EQUAL, home2nd);
            case HOME_GOALS_FIRST_HALF_LESS_THAN_SECOND -> evaluate(home1st, LESS, home2nd);
            case AWAY_GOALS_FIRST_HALF_MORE_THAN_SECOND -> evaluate(away1st, MORE, away2nd);
            case AWAY_GOALS_EQUAL_IN_BOTH_HALVES -> evaluate(away1st, EQUAL, away2nd);
            case AWAY_GOALS_FIRST_HALF_LESS_THAN_SECOND -> evaluate(away1st, LESS, away2nd);

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(double goalsTotal, TotalType type, double line) {
        return switch (type) {
            case UNDER -> goalsTotal < line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
            case OVER -> goalsTotal > line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
        };
    }

    private BetStatus evaluate(double goals1, CompareSign sign, double goals2) {
        return switch (sign) {
            case MORE -> goals1 > goals2 ? BetStatus.WON : BetStatus.LOST;
            case EQUAL -> goals1 == goals2 ? BetStatus.WON : BetStatus.LOST;
            case LESS -> goals1 < goals2 ? BetStatus.WON : BetStatus.LOST;
        };
    }
}
