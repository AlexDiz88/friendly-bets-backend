package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.BetTitleCode;
import net.friendly_bets.models.GameScore;

public interface BetChecker {
    Bet.BetStatus check(GameScore result, BetTitleCode code);
}
