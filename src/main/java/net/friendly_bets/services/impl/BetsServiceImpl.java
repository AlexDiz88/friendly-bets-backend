package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.DeletedBetDto;
import net.friendly_bets.dto.EditedCompleteBetDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.PlayerStats;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.PlayerStatsRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.BetsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.friendly_bets.utils.BetValuesUtils.*;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetsServiceImpl implements BetsService {

    BetsRepository betsRepository;
    TeamsRepository teamsRepository;
    UsersRepository usersRepository;
    PlayerStatsRepository playerStatsRepository;

    @Override
    public BetsPage getAllBets() {
        List<Bet> allBets = betsRepository.findAll();
        return BetsPage.builder()
//                .bets(BetDto.from(allBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto editBet(String moderatorId, String betId, EditedCompleteBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, editedBet.getUserId());
        Team homeTeam = getTeamOrThrow(teamsRepository, editedBet.getHomeTeamId());
        Team awayTeam = getTeamOrThrow(teamsRepository, editedBet.getAwayTeamId());
        Bet bet = getBetOrThrow(betsRepository, betId);
        Bet previousBet = Bet.builder()
                .user(bet.getUser())
                .betOdds(bet.getBetOdds())
                .betStatus(bet.getBetStatus())
                .balanceChange(bet.getBalanceChange())
                .betSize(bet.getBetSize())
                .build();

        if (bet.getBetStatus() == Bet.BetStatus.WON || bet.getBetStatus() == Bet.BetStatus.RETURNED || bet.getBetStatus() == Bet.BetStatus.LOST) {
            try {
                Bet.BetStatus.valueOf(editedBet.getBetStatus());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Недопустимый статус: " + editedBet.getBetStatus());
            }

            if (bet.getGameResult() == null || bet.getGameResult().isBlank()) {
                throw new BadRequestException("Счёт матча отсутствует");
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
            setBalanceChange(bet, status, editedBet.getBetSize(), editedBet.getBetOdds());

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

        if (user.equals(previousBet.getUser()) && !bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), user);
            PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), user));
            if (previousBet.getBetStatus().equals(bet.getBetStatus()) && !previousBet.getBetOdds().equals(bet.getBetOdds())) {
                playerStats.setSumOfOdds(playerStats.getSumOfOdds() - previousBet.getBetOdds() + bet.getBetOdds());
                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - previousBet.getBetOdds() + bet.getBetOdds());
                    playerStats.calculateAverageWonBetOdds();
                }
            }
            if (!previousBet.getBetStatus().equals(bet.getBetStatus())) {
                if (!previousBet.getBetOdds().equals(bet.getBetOdds())) {
                    playerStats.setSumOfOdds(playerStats.getSumOfOdds() - previousBet.getBetOdds() + bet.getBetOdds());
                }
                if (previousBet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    playerStats.setWonBetCount(playerStats.getWonBetCount() - 1);
                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - previousBet.getBetOdds());
                }
                if (previousBet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                    playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() - 1);
                }
                if (previousBet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                    playerStats.setLostBetCount(playerStats.getLostBetCount() - 1);
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + previousBet.getBetOdds());
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                    playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
                }
                if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                    playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
                }
            }
            playerStats.setActualBalance(playerStats.getActualBalance() - previousBet.getBalanceChange() + bet.getBalanceChange());
            playerStats.calculateWinRate();
            playerStats.calculateAverageOdds();
            playerStats.calculateAverageWonBetOdds();
            playerStatsRepository.save(playerStats);
        }

        if (!user.equals(previousBet.getUser()) && !previousBet.getBetStatus().equals(Bet.BetStatus.OPENED) && !bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            Optional<PlayerStats> previousPlayerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), previousBet.getUser());
            PlayerStats previousPlayerStats = previousPlayerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), previousBet.getUser()));
            previousPlayerStats.setTotalBets(previousPlayerStats.getTotalBets() - 1);
            previousPlayerStats.setBetCount(previousPlayerStats.getBetCount() - 1);
            previousPlayerStats.setActualBalance(previousPlayerStats.getActualBalance() - previousBet.getBalanceChange());
            previousPlayerStats.setSumOfOdds(previousPlayerStats.getSumOfOdds() - previousBet.getBetOdds());
            if (previousBet.getBetStatus().equals(Bet.BetStatus.WON)) {
                previousPlayerStats.setWonBetCount(previousPlayerStats.getWonBetCount() - 1);
                previousPlayerStats.setSumOfWonOdds(previousPlayerStats.getSumOfWonOdds() - previousBet.getBetOdds());
            }
            if (previousBet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                previousPlayerStats.setReturnedBetCount(previousPlayerStats.getReturnedBetCount() - 1);
            }
            if (previousBet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                previousPlayerStats.setLostBetCount(previousPlayerStats.getLostBetCount() - 1);
            }
            previousPlayerStats.calculateWinRate();
            previousPlayerStats.calculateAverageOdds();
            previousPlayerStats.calculateAverageWonBetOdds();
            playerStatsRepository.save(previousPlayerStats);

            Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), user);
            PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), user));
            playerStats.setTotalBets(playerStats.getTotalBets() + 1);
            playerStats.setBetCount(playerStats.getBetCount() + 1);
            playerStats.setActualBalance(playerStats.getActualBalance() + bet.getBalanceChange());
            playerStats.setSumOfOdds(playerStats.getSumOfOdds() + bet.getBetOdds());
            if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
                playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
                playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + bet.getBetOdds());
            }
            if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
            }
            if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
            }
            playerStats.calculateWinRate();
            playerStats.calculateAverageOdds();
            playerStats.calculateAverageWonBetOdds();
            playerStatsRepository.save(playerStats);
        }
        if (!user.equals(previousBet.getUser()) && previousBet.getBetStatus().equals(Bet.BetStatus.OPENED) && bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            Optional<PlayerStats> previousPlayerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), previousBet.getUser());
            PlayerStats previousPlayerStats = previousPlayerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), previousBet.getUser()));
            previousPlayerStats.setTotalBets(previousPlayerStats.getTotalBets() - 1);
            playerStatsRepository.save(previousPlayerStats);

            Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), user);
            PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), user));
            playerStats.setTotalBets(playerStats.getTotalBets() + 1);
            playerStatsRepository.save(playerStats);
        }

        return BetDto.from(editedBet.getSeasonId(), editedBet.getLeagueId(), bet);
    }

    @Override
    @Transactional
    public BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData) {
        Bet bet = getBetOrThrow(betsRepository, betId);
        User moderator = getUserOrThrow(usersRepository, moderatorId);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(deletedBetMetaData.getSeasonId(), deletedBetMetaData.getLeagueId(), bet.getUser());
        if (playerStatsOptional.isEmpty()) {
            throw new BadRequestException("Статистики участника с указанными данными не существует");
        }
        PlayerStats playerStats = playerStatsOptional.get();

        playerStats.setTotalBets(playerStats.getTotalBets() - 1);
        System.out.println("betStatus:" + bet.getBetStatus());

        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            playerStats.setBetCount(playerStats.getBetCount() - 1);
            playerStats.setActualBalance(playerStats.getActualBalance() - bet.getBalanceChange());
        }
        if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY)) {
            playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() - 1);
        }
        if (bet.getBetStatus().equals(Bet.BetStatus.WON)) {
            playerStats.setWonBetCount(playerStats.getWonBetCount() - 1);
            playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());
            playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - bet.getBetOdds());
        }
        if (bet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
            playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() - 1);
            playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());
        }
        if (bet.getBetStatus().equals(Bet.BetStatus.LOST)) {
            playerStats.setLostBetCount(playerStats.getLostBetCount() - 1);
            playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());
        }
        playerStats.calculateWinRate();
        playerStats.calculateAverageOdds();
        playerStats.calculateAverageWonBetOdds();
        playerStatsRepository.save(playerStats);

        if (bet.getBetStatus() != Bet.BetStatus.OPENED && bet.getBetStatus() != Bet.BetStatus.DELETED) {
            bet.setBalanceChange(0.0);
        }

        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        bet.setBetStatus(Bet.BetStatus.DELETED);

        betsRepository.save(bet);

        return BetDto.from(deletedBetMetaData.getSeasonId(), deletedBetMetaData.getLeagueId(), bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

}
