package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.BetTitleCode;
import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.TotalType;

import static net.friendly_bets.utils.BetCheckUtils.TotalType.OVER;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.UNDER;

public class TotalChecker implements BetChecker {

    @Override
    public BetStatus check(GameResult gameResult, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameResult);
        double home = gameScores.getHomeFullTime();
        double away = gameScores.getAwayFullTime();
        double total = home + away;

        return switch (code) {
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

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(double goalsTotal, TotalType type, double line) {
        return switch (type) {
            case UNDER -> goalsTotal < line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
            case OVER -> goalsTotal > line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
        };
    }
}
