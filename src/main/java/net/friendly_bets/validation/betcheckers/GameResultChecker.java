package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.BetTitleCode;
import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameResult;
import net.friendly_bets.utils.BetCheckUtils;

public class GameResultChecker implements BetChecker {

    @Override
    public BetStatus check(GameResult gameResult, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameResult);
        int home = gameScores.getHomeFullTime();
        int away = gameScores.getAwayFullTime();

        return switch (code) {
            case RESULT_HOME_WIN -> home > away ? BetStatus.WON : BetStatus.LOST;
            case RESULT_DRAW -> home == away ? BetStatus.WON : BetStatus.LOST;
            case RESULT_AWAY_WIN -> home < away ? BetStatus.WON : BetStatus.LOST;
            case DOUBLE_CHANCE_HOME_OR_DRAW -> (home >= away) ? BetStatus.WON : BetStatus.LOST;
            case DOUBLE_CHANCE_DRAW_OR_AWAY -> (home <= away) ? BetStatus.WON : BetStatus.LOST;
            case DOUBLE_CHANCE_HOME_OR_AWAY -> (home != away) ? BetStatus.WON : BetStatus.LOST;
            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }
}
