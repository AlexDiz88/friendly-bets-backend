package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.MatchResult;
import net.friendly_bets.utils.BetCheckUtils.TotalType;

import static net.friendly_bets.utils.BetCheckUtils.MatchResult.*;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.OVER;
import static net.friendly_bets.utils.BetCheckUtils.TotalType.UNDER;

public class GoalsChecker implements BetChecker {

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
        boolean bothScores = home > 0 && away > 0;

        return switch (code) {
            case BOTH_TEAMS_SCORE -> evaluate(home, away);
            case HOME_TEAM_SCORES -> evaluate(home);
            case AWAY_TEAM_SCORES -> evaluate(away);

            case HOME_SCORES_1ST_HALF -> evaluate(home1st);
            case HOME_SCORES_2ND_HALF -> evaluate(home2nd);
            case AWAY_SCORES_1ST_HALF -> evaluate(away1st);
            case AWAY_SCORES_2ND_HALF -> evaluate(away2nd);
            case HOME_SCORES_BOTH_HALVES -> evaluate(home1st, home2nd);
            case AWAY_SCORES_BOTH_HALVES -> evaluate(away1st, away2nd);
            case BOTH_TEAMS_SCORE_1ST_HALF -> evaluate(home1st, away1st);
            case BOTH_TEAMS_SCORE_2ND_HALF -> evaluate(home2nd, away2nd);
            case BOTH_TEAMS_SCORE_BOTH_HALVES -> home1st > 0 && home2nd > 0 && away1st > 0 && away2nd > 0 ? BetStatus.WON : BetStatus.LOST;
            case GOALS_IN_BOTH_HALVES -> (home1st > 0 || away1st > 0) && (home2nd > 0 || away2nd > 0) ? BetStatus.WON : BetStatus.LOST;

            case HOME_WIN_AND_BOTH_TEAMS_SCORE -> evaluate(HOME_WIN, home, away);
            case DRAW_AND_BOTH_TEAMS_SCORE -> evaluate(DRAW, home, away);
            case AWAY_WIN_AND_BOTH_TEAMS_SCORE -> evaluate(AWAY_WIN, home, away);
            case HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE -> evaluate(HOME_WIN_OR_DRAW, home, away);
            case HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE -> evaluate(HOME_OR_AWAY_WIN, home, away);
            case AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE -> evaluate(AWAY_WIN_OR_DRAW, home, away);

            case ANY_TEAM_SCORES_2_OR_MORE -> evaluate(home, away, 2);
            case ANY_TEAM_SCORES_3_OR_MORE -> evaluate(home, away, 3);
            case ANY_TEAM_SCORES_4_OR_MORE -> evaluate(home, away, 4);
            case ANY_TEAM_SCORES_5_OR_MORE -> evaluate(home, away, 5);

            case BOTH_TEAMS_SCORE_AND_UNDER_1_5 -> evaluate(bothScores, total, UNDER, 1.5);
            case BOTH_TEAMS_SCORE_AND_UNDER_2_0 -> evaluate(bothScores, total, UNDER, 2.0);
            case BOTH_TEAMS_SCORE_AND_UNDER_2_5 -> evaluate(bothScores, total, UNDER, 2.5);
            case BOTH_TEAMS_SCORE_AND_UNDER_3_0 -> evaluate(bothScores, total, UNDER, 3.0);
            case BOTH_TEAMS_SCORE_AND_UNDER_3_5 -> evaluate(bothScores, total, UNDER, 3.5);
            case BOTH_TEAMS_SCORE_AND_UNDER_4_0 -> evaluate(bothScores, total, UNDER, 4.0);
            case BOTH_TEAMS_SCORE_AND_UNDER_4_5 -> evaluate(bothScores, total, UNDER, 4.5);
            case BOTH_TEAMS_SCORE_AND_UNDER_5_0 -> evaluate(bothScores, total, UNDER, 5.0);
            case BOTH_TEAMS_SCORE_AND_UNDER_5_5 -> evaluate(bothScores, total, UNDER, 5.5);
            case BOTH_TEAMS_SCORE_AND_UNDER_6_0 -> evaluate(bothScores, total, UNDER, 6.0);
            case BOTH_TEAMS_SCORE_AND_UNDER_6_5 -> evaluate(bothScores, total, UNDER, 6.5);

            case BOTH_TEAMS_SCORE_AND_OVER_1_5 -> evaluate(bothScores, total, OVER, 1.5);
            case BOTH_TEAMS_SCORE_AND_OVER_2_0 -> evaluate(bothScores, total, OVER, 2.0);
            case BOTH_TEAMS_SCORE_AND_OVER_2_5 -> evaluate(bothScores, total, OVER, 2.5);
            case BOTH_TEAMS_SCORE_AND_OVER_3_0 -> evaluate(bothScores, total, OVER, 3.0);
            case BOTH_TEAMS_SCORE_AND_OVER_3_5 -> evaluate(bothScores, total, OVER, 3.5);
            case BOTH_TEAMS_SCORE_AND_OVER_4_0 -> evaluate(bothScores, total, OVER, 4.0);
            case BOTH_TEAMS_SCORE_AND_OVER_4_5 -> evaluate(bothScores, total, OVER, 4.5);
            case BOTH_TEAMS_SCORE_AND_OVER_5_0 -> evaluate(bothScores, total, OVER, 5.0);
            case BOTH_TEAMS_SCORE_AND_OVER_5_5 -> evaluate(bothScores, total, OVER, 5.5);
            case BOTH_TEAMS_SCORE_AND_OVER_6_0 -> evaluate(bothScores, total, OVER, 6.0);
            case BOTH_TEAMS_SCORE_AND_OVER_6_5 -> evaluate(bothScores, total, OVER, 6.5);

            case FIRST_HALF_BOTH_SCORE_AND_HOME_WIN -> evaluate(HOME_WIN, home1st, away1st);
            case FIRST_HALF_BOTH_SCORE_AND_DRAW -> evaluate(DRAW, home1st, away1st);
            case FIRST_HALF_BOTH_SCORE_AND_AWAY_WIN -> evaluate(AWAY_WIN, home1st, away1st);
            case FIRST_HALF_BOTH_SCORE_AND_HOME_OR_DRAW -> evaluate(HOME_WIN_OR_DRAW, home1st, away1st);
            case FIRST_HALF_BOTH_SCORE_AND_HOME_OR_AWAY -> evaluate(HOME_OR_AWAY_WIN, home1st, away1st);
            case FIRST_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW -> evaluate(AWAY_WIN_OR_DRAW, home1st, away1st);

            case SECOND_HALF_BOTH_SCORE_AND_HOME_WIN -> evaluate(HOME_WIN, home2nd, away2nd);
            case SECOND_HALF_BOTH_SCORE_AND_DRAW -> evaluate(DRAW, home2nd, away2nd);
            case SECOND_HALF_BOTH_SCORE_AND_AWAY_WIN -> evaluate(AWAY_WIN, home2nd, away2nd);
            case SECOND_HALF_BOTH_SCORE_AND_HOME_OR_DRAW -> evaluate(HOME_WIN_OR_DRAW, home2nd, away2nd);
            case SECOND_HALF_BOTH_SCORE_AND_HOME_OR_AWAY -> evaluate(HOME_OR_AWAY_WIN, home2nd, away2nd);
            case SECOND_HALF_BOTH_SCORE_AND_AWAY_OR_DRAW -> evaluate(AWAY_WIN_OR_DRAW, home2nd, away2nd);


            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(int goals) {
        return goals > 0 ? BetStatus.WON : BetStatus.LOST;
    }

    private BetStatus evaluate(int goals1, int goals2) {
        return goals1 > 0 && goals2 > 0 ? BetStatus.WON : BetStatus.LOST;
    }

    private BetStatus evaluate(MatchResult expectedResult, int home, int away) {
        boolean gameResult = BetCheckUtils.checkBetGameResult(home, away, expectedResult);
        if (!gameResult)
            return BetStatus.LOST;

        return home > 0 && away > 0 ? BetStatus.WON : BetStatus.LOST;
    }

    private BetStatus evaluate(int home, int away, int goalsAmount) {
        return home > goalsAmount || away > goalsAmount ? BetStatus.WON : BetStatus.LOST;
    }

    private BetStatus evaluate(boolean bothScores, double goalsTotal, TotalType type, double line) {
        if (!bothScores)
            return BetStatus.LOST;

        return switch (type) {
            case UNDER -> goalsTotal < line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
            case OVER -> goalsTotal > line ? BetStatus.WON : goalsTotal == line ? BetStatus.RETURNED : BetStatus.LOST;
        };
    }
}
