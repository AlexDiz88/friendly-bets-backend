package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.BetTitleCode;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;

import static net.friendly_bets.models.Bet.BetStatus.WON;

public class GameResultChecker implements BetChecker{

    @Override
    public Bet.BetStatus check(GameResult result, short code) {
        return WON;
    }
}
