package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.models.Bet;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.BetsService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BetsServiceImpl implements BetsService {

    private final BetsRepository betsRepository;

    @Override
    public BetsPage getAllBets() {
        List<Bet> allBets = betsRepository.findAll();
        return BetsPage.builder()
                .bets(BetDto.from(allBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

//    @Override
//    public BetDto deleteBet(String betId) {
//        Bet bet = betsRepository.findById(betId).orElseThrow(
//                () -> new NotFoundException("Ставка с ID <" + betId + "> не найдена"));
//        betsRepository.delete(bet);
//        return BetDto.from(bet);
//    }
}
