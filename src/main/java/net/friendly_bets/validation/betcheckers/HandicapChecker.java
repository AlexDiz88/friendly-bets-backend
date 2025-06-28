package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.BetTitleCode;
import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.utils.BetCheckUtils;
import net.friendly_bets.utils.BetCheckUtils.HandicapType;
import net.friendly_bets.utils.BetCheckUtils.MatchResult;

import static net.friendly_bets.utils.BetCheckUtils.HandicapType.MINUS;
import static net.friendly_bets.utils.BetCheckUtils.HandicapType.PLUS;
import static net.friendly_bets.utils.BetCheckUtils.MatchResult.AWAY_WIN;
import static net.friendly_bets.utils.BetCheckUtils.MatchResult.HOME_WIN;

public class HandicapChecker implements BetChecker {

    @Override
    public BetStatus check(GameResult gameResult, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameResult);
        double home = gameScores.getHomeFullTime();
        double away = gameScores.getAwayFullTime();

        return switch (code) {
            case HANDICAP_HOME_0 -> evaluate(home, away, HOME_WIN, PLUS, 0.0);
            case HANDICAP_HOME_MINUS_1_0 -> evaluate(home, away, HOME_WIN, MINUS, 1.0);
            case HANDICAP_HOME_PLUS_1_0 -> evaluate(home, away, HOME_WIN, PLUS, 1.0);
            case HANDICAP_HOME_MINUS_1_5 -> evaluate(home, away, HOME_WIN, MINUS, 1.5);
            case HANDICAP_HOME_PLUS_1_5 -> evaluate(home, away, HOME_WIN, PLUS, 1.5);
            case HANDICAP_HOME_MINUS_2_0 -> evaluate(home, away, HOME_WIN, MINUS, 2.0);
            case HANDICAP_HOME_PLUS_2_0 -> evaluate(home, away, HOME_WIN, PLUS, 2.0);
            case HANDICAP_HOME_MINUS_2_5 -> evaluate(home, away, HOME_WIN, MINUS, 2.5);
            case HANDICAP_HOME_PLUS_2_5 -> evaluate(home, away, HOME_WIN, PLUS, 2.5);
            case HANDICAP_HOME_MINUS_3_0 -> evaluate(home, away, HOME_WIN, MINUS, 3.0);
            case HANDICAP_HOME_PLUS_3_0 -> evaluate(home, away, HOME_WIN, PLUS, 3.0);
            case HANDICAP_HOME_MINUS_3_5 -> evaluate(home, away, HOME_WIN, MINUS, 3.5);
            case HANDICAP_HOME_PLUS_3_5 -> evaluate(home, away, HOME_WIN, PLUS, 3.5);
            case HANDICAP_HOME_MINUS_4_0 -> evaluate(home, away, HOME_WIN, MINUS, 4.0);
            case HANDICAP_HOME_PLUS_4_0 -> evaluate(home, away, HOME_WIN, PLUS, 4.0);
            case HANDICAP_HOME_MINUS_4_5 -> evaluate(home, away, HOME_WIN, MINUS, 4.5);
            case HANDICAP_HOME_PLUS_4_5 -> evaluate(home, away, HOME_WIN, PLUS, 4.5);
            case HANDICAP_HOME_MINUS_5_0 -> evaluate(home, away, HOME_WIN, MINUS, 5.0);
            case HANDICAP_HOME_PLUS_5_0 -> evaluate(home, away, HOME_WIN, PLUS, 5.0);
            case HANDICAP_HOME_MINUS_5_5 -> evaluate(home, away, HOME_WIN, MINUS, 5.5);
            case HANDICAP_HOME_PLUS_5_5 -> evaluate(home, away, HOME_WIN, PLUS, 5.5);
            case HANDICAP_HOME_MINUS_6_0 -> evaluate(home, away, HOME_WIN, MINUS, 6.0);
            case HANDICAP_HOME_PLUS_6_0 -> evaluate(home, away, HOME_WIN, PLUS, 6.0);

            case HANDICAP_AWAY_0 -> evaluate(home, away, AWAY_WIN, PLUS, 0.0);
            case HANDICAP_AWAY_MINUS_1_0 -> evaluate(home, away, AWAY_WIN, MINUS, 1.0);
            case HANDICAP_AWAY_PLUS_1_0 -> evaluate(home, away, AWAY_WIN, PLUS, 1.0);
            case HANDICAP_AWAY_MINUS_1_5 -> evaluate(home, away, AWAY_WIN, MINUS, 1.5);
            case HANDICAP_AWAY_PLUS_1_5 -> evaluate(home, away, AWAY_WIN, PLUS, 1.5);
            case HANDICAP_AWAY_MINUS_2_0 -> evaluate(home, away, AWAY_WIN, MINUS, 2.0);
            case HANDICAP_AWAY_PLUS_2_0 -> evaluate(home, away, AWAY_WIN, PLUS, 2.0);
            case HANDICAP_AWAY_MINUS_2_5 -> evaluate(home, away, AWAY_WIN, MINUS, 2.5);
            case HANDICAP_AWAY_PLUS_2_5 -> evaluate(home, away, AWAY_WIN, PLUS, 2.5);
            case HANDICAP_AWAY_MINUS_3_0 -> evaluate(home, away, AWAY_WIN, MINUS, 3.0);
            case HANDICAP_AWAY_PLUS_3_0 -> evaluate(home, away, AWAY_WIN, PLUS, 3.0);
            case HANDICAP_AWAY_MINUS_3_5 -> evaluate(home, away, AWAY_WIN, MINUS, 3.5);
            case HANDICAP_AWAY_PLUS_3_5 -> evaluate(home, away, AWAY_WIN, PLUS, 3.5);
            case HANDICAP_AWAY_MINUS_4_0 -> evaluate(home, away, AWAY_WIN, MINUS, 4.0);
            case HANDICAP_AWAY_PLUS_4_0 -> evaluate(home, away, AWAY_WIN, PLUS, 4.0);
            case HANDICAP_AWAY_MINUS_4_5 -> evaluate(home, away, AWAY_WIN, MINUS, 4.5);
            case HANDICAP_AWAY_PLUS_4_5 -> evaluate(home, away, AWAY_WIN, PLUS, 4.5);
            case HANDICAP_AWAY_MINUS_5_0 -> evaluate(home, away, AWAY_WIN, MINUS, 5.0);
            case HANDICAP_AWAY_PLUS_5_0 -> evaluate(home, away, AWAY_WIN, PLUS, 5.0);
            case HANDICAP_AWAY_MINUS_5_5 -> evaluate(home, away, AWAY_WIN, MINUS, 5.5);
            case HANDICAP_AWAY_PLUS_5_5 -> evaluate(home, away, AWAY_WIN, PLUS, 5.5);
            case HANDICAP_AWAY_MINUS_6_0 -> evaluate(home, away, AWAY_WIN, MINUS, 6.0);
            case HANDICAP_AWAY_PLUS_6_0 -> evaluate(home, away, AWAY_WIN, PLUS, 6.0);

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private BetStatus evaluate(double home, double away, MatchResult expectedResult, HandicapType handicapType, double line) {
        double adjustedHome = home;
        double adjustedAway = away;

        if (expectedResult == MatchResult.HOME_WIN) {
            if (handicapType == PLUS) adjustedHome += line;
            if (handicapType == MINUS) adjustedHome -= line;
        }

        if (expectedResult == MatchResult.AWAY_WIN) {
            if (handicapType == PLUS) adjustedAway += line;
            if (handicapType == MINUS) adjustedAway -= line;
        }

        if (adjustedHome > adjustedAway && expectedResult == MatchResult.HOME_WIN) return BetStatus.WON;
        if (adjustedAway > adjustedHome && expectedResult == MatchResult.AWAY_WIN) return BetStatus.WON;
        if (adjustedHome == adjustedAway) return BetStatus.RETURNED;

        return BetStatus.LOST;
    }
}
