package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.BetTitleCode;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameResult;

public interface BetChecker {
    Bet.BetStatus check(GameResult result, short code);
}
