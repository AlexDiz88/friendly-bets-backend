package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.enums.BetTitleCode;

public interface BetChecker {
    Bet.BetStatus check(GameScore result, BetTitleCode code);
}
