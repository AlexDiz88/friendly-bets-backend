package net.friendly_bets.services;

import net.friendly_bets.dto.*;
import org.springframework.data.domain.Pageable;

public interface BetsService {

    BetDto addOpenedBet(String moderatorId, NewBet newBet);

    BetDto addEmptyBet(String moderatorId, NewEmptyBet newEmptyBet);

    BetDto setBetResult(String moderatorId, String betId, BetResult betResult);

    BetsPage getOpenedBets(String seasonId);

    BetsPage getCompletedBets(String seasonId, String playerName, String leagueName, Pageable pageable);

    BetsPage getAllBets(String seasonId, Pageable pageable);

    BetDto editBet(String moderatorId, String betId, EditedBetDto editedBet);

    BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData);
}
