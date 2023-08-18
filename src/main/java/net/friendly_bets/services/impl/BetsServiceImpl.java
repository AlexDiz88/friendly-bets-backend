package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.EditedBetDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.BetsService;
import net.friendly_bets.utils.GameResultValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public BetDto editBet(String moderatorId, String betId, EditedBetDto editedBet) {
        if (editedBet == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (editedBet.getMatchDay() == null || editedBet.getMatchDay().isBlank()) {
            throw new BadDataException("Игровой тур не указан");
        }
        if (editedBet.getHomeTeamId().equals(editedBet.getAwayTeamId())) {
            throw new BadDataException("Команда хозяев не может совпадать с командой гостей");
        }
        if (editedBet.getBetTitle() == null || editedBet.getBetTitle().isBlank()) {
            throw new BadDataException("Ставка не указана");
        }
        if (editedBet.getBetOdds() == null) {
            throw new BadDataException("Коэффициент ставки не указан, либо указан неверно");
        }
        if (editedBet.getBetOdds().isNaN()) {
            throw new BadDataException("Коэффициент ставки не является числом");
        }
        if (editedBet.getBetOdds() <= 1) {
            throw new BadDataException("Коэффициент ставки не может быть меньше чем 1,01");
        }
        if (editedBet.getBetSize() == null) {
            throw new BadDataException("Размер ставки не указан");
        }
        if (editedBet.getBetSize() < 1) {
            throw new BadDataException("Размер ставки не может быть меньше 1");
        }

        User moderator = usersRepository.findById(moderatorId).orElseThrow(
                () -> new NotFoundException("Модератор с таким ID не найден")
        );
        User user = usersRepository.findById(editedBet.getUserId()).orElseThrow(
                () -> new NotFoundException("Участник с таким ID не найден")
        );

        Team homeTeam = teamsRepository.findById(editedBet.getHomeTeamId()).orElseThrow(
                () -> new NotFoundException("Команда хозяев с ID <" + editedBet.getHomeTeamId() + "> не найдена")
        );
        Team awayTeam = teamsRepository.findById(editedBet.getAwayTeamId()).orElseThrow(
                () -> new NotFoundException("Команда гостей с ID <" + editedBet.getAwayTeamId() + "> не найдена")
        );

        Bet bet = betsRepository.findById(betId).orElseThrow(
                () -> new NotFoundException("Ставка с таким ID не найдена"));

        if (bet.getBetStatus() == Bet.BetStatus.WON || bet.getBetStatus() == Bet.BetStatus.RETURNED || bet.getBetStatus() == Bet.BetStatus.LOST) {
            if (editedBet.getBetStatus() == null || editedBet.getBetStatus().isBlank()) {
                throw new BadDataException("Статус ставки не может быть пустым");
            }
            try {
                Bet.BetStatus.valueOf(editedBet.getBetStatus());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Недопустимый статус: " + editedBet.getBetStatus());
            }

            if (bet.getGameResult() != null) {
                if (editedBet.getGameResult() == null || editedBet.getGameResult().isBlank()) {
                    throw new BadDataException("Счёт матча не может быть пустым");
                }
                if (!GameResultValidator.isValidGameResult(editedBet.getGameResult())) {
                    throw new BadDataException("Некорректный счёт матча: " + editedBet.getGameResult());
                }
            }

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
            if (status.equals(Bet.BetStatus.WON)) {
                bet.setBalanceChange(editedBet.getBetOdds() * editedBet.getBetSize() - editedBet.getBetSize());
            }
            if (status.equals(Bet.BetStatus.RETURNED)) {
                bet.setBalanceChange(0.0);
            }
            if (status.equals(Bet.BetStatus.LOST)) {
                bet.setBalanceChange(-Double.valueOf(editedBet.getBetSize()));
            }

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
        Bet bet = betsRepository.findById(betId).orElseThrow(
                () -> new NotFoundException("Ставка с таким ID не найдена"));

        User moderator = usersRepository.findById(moderatorId).orElseThrow(
                () -> new NotFoundException("Модератор с таким ID не найден")
        );

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
