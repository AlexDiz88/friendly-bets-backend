package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.BetTitleCode;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.MatchResult;

import static net.friendly_bets.utils.BetCheckUtils.MatchResult.*;

public class GameResultChecker implements BetChecker {

    @Override
    public BetStatus check(GameScore gameScore, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameScore);
        int home = gameScores.getHomeFullTime();
        int home1st = gameScores.getHomeFirstHalf();
        int home2nd = home - home1st;
        int away = gameScores.getAwayFullTime();
        int away1st = gameScores.getAwayFirstHalf();
        int away2nd = away - away1st;

        return switch (code) {
            case EMPTY_BET_TITLE -> BetStatus.EMPTY;
            case HOME_WIN -> evaluate(home, away, HOME_WIN);
            case DRAW -> evaluate(home, away, DRAW);
            case AWAY_WIN -> evaluate(home, away, AWAY_WIN);
            case HOME_WIN_OR_DRAW -> evaluate(home, away, HOME_WIN_OR_DRAW);
            case HOME_OR_AWAY_WIN -> evaluate(home, away, HOME_OR_AWAY_WIN);
            case AWAY_WIN_OR_DRAW -> evaluate(home, away, AWAY_WIN_OR_DRAW);

            case FIRST_HALF_HOME_WIN -> evaluate(home1st, away1st, HOME_WIN);
            case FIRST_HALF_DRAW -> evaluate(home1st, away1st, DRAW);
            case FIRST_HALF_AWAY_WIN -> evaluate(home1st, away1st, AWAY_WIN);
            case FIRST_HALF_HOME_WIN_OR_DRAW -> evaluate(home1st, away1st, HOME_WIN_OR_DRAW);
            case FIRST_HALF_HOME_OR_AWAY_WIN -> evaluate(home1st, away1st, HOME_OR_AWAY_WIN);
            case FIRST_HALF_AWAY_WIN_OR_DRAW -> evaluate(home1st, away1st, AWAY_WIN_OR_DRAW);

            case SECOND_HALF_HOME_WIN -> evaluate(home2nd, away2nd, HOME_WIN);
            case SECOND_HALF_DRAW -> evaluate(home2nd, away2nd, DRAW);
            case SECOND_HALF_AWAY_WIN -> evaluate(home2nd, away2nd, AWAY_WIN);
            case SECOND_HALF_HOME_WIN_OR_DRAW -> evaluate(home2nd, away2nd, HOME_WIN_OR_DRAW);
            case SECOND_HALF_HOME_OR_AWAY_WIN -> evaluate(home2nd, away2nd, HOME_OR_AWAY_WIN);
            case SECOND_HALF_AWAY_WIN_OR_DRAW -> evaluate(home2nd, away2nd, AWAY_WIN_OR_DRAW);

            case ANY_HALF_HOME_WIN -> (home1st > away1st) || (home2nd > away2nd) ? BetStatus.WON : BetStatus.LOST;
            case ANY_HALF_DRAW -> (home1st == away1st) || (home2nd == away2nd) ? BetStatus.WON : BetStatus.LOST;
            case ANY_HALF_AWAY_WIN -> (home1st < away1st) || (home2nd < away2nd) ? BetStatus.WON : BetStatus.LOST;

            case HALF_FULL_HOME_HOME -> evaluate(home1st, away1st, HOME_WIN, home, away, HOME_WIN);
            case HALF_FULL_HOME_DRAW -> evaluate(home1st, away1st, HOME_WIN, home, away, DRAW);
            case HALF_FULL_HOME_AWAY -> evaluate(home1st, away1st, HOME_WIN, home, away, AWAY_WIN);
            case HALF_FULL_DRAW_HOME -> evaluate(home1st, away1st, DRAW, home, away, HOME_WIN);
            case HALF_FULL_DRAW_DRAW -> evaluate(home1st, away1st, DRAW, home, away, DRAW);
            case HALF_FULL_DRAW_AWAY -> evaluate(home1st, away1st, DRAW, home, away, AWAY_WIN);
            case HALF_FULL_AWAY_HOME -> evaluate(home1st, away1st, AWAY_WIN, home, away, HOME_WIN);
            case HALF_FULL_AWAY_DRAW -> evaluate(home1st, away1st, AWAY_WIN, home, away, DRAW);
            case HALF_FULL_AWAY_AWAY -> evaluate(home1st, away1st, AWAY_WIN, home, away, AWAY_WIN);

            case FIRST_SECOND_HOME_HOME -> evaluate(home1st, away1st, HOME_WIN, home2nd, away2nd, HOME_WIN);
            case FIRST_SECOND_HOME_DRAW -> evaluate(home1st, away1st, HOME_WIN, home2nd, away2nd, DRAW);
            case FIRST_SECOND_HOME_AWAY -> evaluate(home1st, away1st, HOME_WIN, home2nd, away2nd, AWAY_WIN);
            case FIRST_SECOND_DRAW_HOME -> evaluate(home1st, away1st, DRAW, home2nd, away2nd, HOME_WIN);
            case FIRST_SECOND_DRAW_DRAW -> evaluate(home1st, away1st, DRAW, home2nd, away2nd, DRAW);
            case FIRST_SECOND_DRAW_AWAY -> evaluate(home1st, away1st, DRAW, home2nd, away2nd, AWAY_WIN);
            case FIRST_SECOND_AWAY_HOME -> evaluate(home1st, away1st, AWAY_WIN, home2nd, away2nd, HOME_WIN);
            case FIRST_SECOND_AWAY_DRAW -> evaluate(home1st, away1st, AWAY_WIN, home2nd, away2nd, DRAW);
            case FIRST_SECOND_AWAY_AWAY -> evaluate(home1st, away1st, AWAY_WIN, home2nd, away2nd, AWAY_WIN);

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(int home, int away, MatchResult expectedResult) {
        return BetCheckUtils.checkBetGameResult(home, away, expectedResult) ? BetStatus.WON : BetStatus.LOST;
    }

    private BetStatus evaluate(int home1, int away1, MatchResult expectedResult1, int home2, int away2, MatchResult expectedResult2) {
        boolean actualResult1 = BetCheckUtils.checkBetGameResult(home1, away1, expectedResult1);
        boolean actualResult2 = BetCheckUtils.checkBetGameResult(home2, away2, expectedResult2);

        return actualResult1 && actualResult2 ? BetStatus.WON : BetStatus.LOST;
    }
}
