package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.EditedCompleteBetDto;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.BetsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static net.friendly_bets.utils.BetValuesUtils.*;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
public class BetsServiceImpl implements BetsService {

    private final BetsRepository betsRepository;
    private final TeamsRepository teamsRepository;
    private final UsersRepository usersRepository;

    @Override
    public BetsPage getAllBets() {
        List<Bet> allBets = betsRepository.findAll();
        return BetsPage.builder()
                .bets(BetDto.from(allBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetDto editBet(String moderatorId, String betId, EditedCompleteBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, editedBet.getUserId());
        Team homeTeam = getTeamOrThrow(teamsRepository, editedBet.getHomeTeamId());
        Team awayTeam = getTeamOrThrow(teamsRepository, editedBet.getAwayTeamId());
        Bet bet = getBetOrThrow(betsRepository, betId);

        if (bet.getBetStatus() == Bet.BetStatus.WON || bet.getBetStatus() == Bet.BetStatus.RETURNED || bet.getBetStatus() == Bet.BetStatus.LOST) {
            try {
                Bet.BetStatus.valueOf(editedBet.getBetStatus());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Недопустимый статус: " + editedBet.getBetStatus());
            }

            checkGameResult(editedBet.getGameResult());

            if (betsRepository.existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                    user,
                    editedBet.getMatchDay(),
                    homeTeam,
                    awayTeam,
                    editedBet.getBetTitle(),
                    editedBet.getBetOdds(),
                    editedBet.getBetSize(),
                    editedBet.getGameResult(),
                    Bet.BetStatus.valueOf(editedBet.getBetStatus())
            )) {
                throw new ConflictException("Ставка на этот матч уже отредактирована другим модератором");
            }

            Bet.BetStatus status = Bet.BetStatus.valueOf(editedBet.getBetStatus());
            setBalanceChange(bet, status);

            bet.setGameResult(editedBet.getGameResult());
            bet.setBetStatus(Bet.BetStatus.valueOf(editedBet.getBetStatus()));

        } else if (bet.getBetStatus() == Bet.BetStatus.OPENED) {
            if (betsRepository.existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
                    user,
                    editedBet.getMatchDay(),
                    homeTeam,
                    awayTeam,
                    editedBet.getBetTitle(),
                    editedBet.getBetOdds(),
                    editedBet.getBetSize()
            )) {
                throw new ConflictException("Ставка на этот матч уже отредактирована другим модератором");
            }
        }

        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        bet.setUser(user);
        bet.setMatchDay(editedBet.getMatchDay());
        bet.setHomeTeam(homeTeam);
        bet.setAwayTeam(awayTeam);
        bet.setBetTitle(editedBet.getBetTitle());
        bet.setBetOdds(editedBet.getBetOdds());
        bet.setBetSize(editedBet.getBetSize());

        betsRepository.save(bet);
        return BetDto.from(bet);
    }

    @Override
    public BetDto deleteBet(String moderatorId, String betId) {
        Bet bet = getBetOrThrow(betsRepository, betId);
        User moderator = getUserOrThrow(usersRepository, moderatorId);

        if (bet.getBetStatus() != Bet.BetStatus.OPENED && bet.getBetStatus() != Bet.BetStatus.DELETED) {
            bet.setBalanceChange(0.0);
        }

        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        bet.setBetStatus(Bet.BetStatus.DELETED);

        betsRepository.save(bet);
        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
