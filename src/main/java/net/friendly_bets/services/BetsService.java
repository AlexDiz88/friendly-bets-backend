package net.friendly_bets.services;

import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.dto.SeasonDto;

public interface BetsService {

    BetsPage getAllBets();

    BetDto editBet(String moderatorId, String betId, EditedBetDto editedBet);

    BetDto deleteBet(String moderatorId, String betId);
}
