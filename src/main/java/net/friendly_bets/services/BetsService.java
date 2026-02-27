package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.friendly_bets.utils.BetUtils.*;
import static net.friendly_bets.utils.Constants.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetsService {

    LeaguesRepository leaguesRepository;
    BetsRepository betsRepository;

    GetEntityService getEntityService;
    CalendarsService calendarsService;
    PlayerStatsService playerStatsService;
    TeamStatsService teamStatsService;
    GameweekStatsService gameweekStatsService;
    BetTitleStatsService betTitleStatsService;

    @Transactional
    public BetDto addOpenedBet(String moderatorId, NewBetDto newOpenedBet) {
        validateBet(newOpenedBet);
        checkIfBetAlreadyExists(betsRepository, newOpenedBet);

        User moderator = getEntityService.getUserOrThrow(moderatorId);
        User user = getEntityService.getUserOrThrow(newOpenedBet.getUserId());
        Season season = getEntityService.getSeasonOrThrow(newOpenedBet.getSeasonId());
        League league = getEntityService.getLeagueOrThrow(newOpenedBet.getLeagueId());
        Team homeTeam = getEntityService.getTeamOrThrow(newOpenedBet.getHomeTeamId());
        Team awayTeam = getEntityService.getTeamOrThrow(newOpenedBet.getAwayTeamId());

        Bet openedBet = createNewOpenedBet(newOpenedBet, moderator, user, season, league, homeTeam, awayTeam);

        betsRepository.save(openedBet);
        updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);
        calendarsService.addBetToCalendarNode(openedBet, newOpenedBet.getCalendarNodeId(), newOpenedBet.getLeagueId(), newOpenedBet.getMatchDay());
        playerStatsService.calculateStatsBasedOnNewOpenedBet(season.getId(), league.getId(), user, true);

        return BetDto.from(openedBet);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto addEmptyBet(String moderatorId, NewEmptyBet newEmptyBet) {
        User moderator = getEntityService.getUserOrThrow(moderatorId);
        User user = getEntityService.getUserOrThrow(newEmptyBet.getUserId());
        Season season = getEntityService.getSeasonOrThrow(newEmptyBet.getSeasonId());
        League league = getEntityService.getLeagueOrThrow(newEmptyBet.getLeagueId());

        Bet emptyBet = createNewEmptyBet(newEmptyBet, moderator, user, season, league);

        betsRepository.save(emptyBet);
        updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);
        calendarsService.addBetToCalendarNode(emptyBet, newEmptyBet.getCalendarNodeId(), newEmptyBet.getLeagueId(), newEmptyBet.getMatchDay());
        playerStatsService.calculateStatsBasedOnEmptyBet(season.getId(), league.getId(), user, newEmptyBet.getBetSize(), true);
        gameweekStatsService.calculateGameweekStats(newEmptyBet.getCalendarNodeId());

        return BetDto.from(emptyBet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public BetDto setBetResult(String moderatorId, String betId, BetResult betResult) {
        try {
            Bet.BetStatus.valueOf(betResult.getBetStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalidStatus");
        }

        checkGameScore(betResult.getGameScore(), Bet.BetStatus.valueOf(betResult.getBetStatus()));

        User moderator = getEntityService.getUserOrThrow(moderatorId);
        Bet bet = getEntityService.getBetOrThrow(betId);
        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            throw new ConflictException("betAlreadyProcessed");
        }

        processBetResultValues(moderator, bet, betResult);
        betsRepository.save(bet);

        String seasonId = bet.getSeason().getId();
        String leagueId = bet.getLeague().getId();
        String userId = bet.getUser().getId();

        playerStatsService.calculateStatsBasedOnBetResult(seasonId, leagueId, bet.getUser(), bet, true);
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, userId, bet, true);
        gameweekStatsService.calculateGameweekStats(bet.getCalendarNodeId());
        betTitleStatsService.calculateStatsByBetTitle(seasonId, userId, bet, true);

        return BetDto.from(bet);
    }

    @Transactional
    public BetsPage setBetResults(String moderatorId, String seasonId, List<GameResult> gameResults) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        List<Bet> processedBets = new ArrayList<>();

        for (GameResult gameResult : gameResults) {
            List<Bet> matchingBets = findMatchingBets(openedBets, gameResult);

            for (Bet bet : matchingBets) {
                short betTitleCodeNumber = bet.getBetTitle().getCode();
                BetTitleCode betTitleCode = BetTitleCode.fromCode(betTitleCodeNumber);
                GameScore gameScore = gameResult.getGameScore();
                Bet.BetStatus betStatus = betTitleCode.getChecker().check(gameScore, betTitleCode);
                if (betStatus == Bet.BetStatus.OPENED) {
                    continue;
                }

                boolean isNot = bet.getBetTitle().isNot(); // инверсия BetStatus, если установлен флаг "нет"
                if (isNot) {
                    betStatus = invertBetStatus(betStatus);
                }

                BetResult betResult = BetResult.builder()
                        .gameScore(gameScore)
                        .betStatus(betStatus.name())
                        .build();

                setBetResult(moderatorId, bet.getId(), betResult);
                processedBets.add(bet);
            }
        }

        return BetsPage.builder()
                .bets(BetDto.from(processedBets))
                .build();
    }

    private List<Bet> findMatchingBets(List<Bet> bets, GameResult gameResult) {
        return bets.stream()
                .filter(bet ->
                        bet.getLeague().getId().equals(gameResult.getLeagueId()) &&
                                bet.getHomeTeam().getId().equals(gameResult.getHomeTeamId()) &&
                                bet.getAwayTeam().getId().equals(gameResult.getAwayTeamId())
                )
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getOpenedBets(String seasonId) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        return BetsPage.builder()
                .bets(BetDto.from(openedBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getCompletedBets(String seasonId, String playerId, String leagueId, Pageable pageable) {
        Page<Bet> completedBetsPage = null;

        if (leagueId == null && playerId == null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusIn(seasonId, COMPLETED_BET_STATUSES, pageable);
        }
        if (leagueId == null && playerId != null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndUser_Id(seasonId, COMPLETED_BET_STATUSES, playerId, pageable);
        }
        if (leagueId != null && playerId == null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndLeague_Id(seasonId, COMPLETED_BET_STATUSES, leagueId, pageable);
        }
        if (leagueId != null && playerId != null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndLeague_IdAndUser_Id(seasonId, COMPLETED_BET_STATUSES, leagueId, playerId, pageable);
        }
        if (completedBetsPage == null) {
            throw new BadRequestException("invalidRequest");
        }

        return BetsPage.builder()
                .bets(BetDto.from(completedBetsPage.getContent()))
                .totalPages(completedBetsPage.getTotalPages())
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getAllBets(String seasonId, Pageable pageable) {
        Page<Bet> allBets = betsRepository.findAllBySeason_IdAndBetStatusIn(seasonId, VALID_BET_STATUSES, pageable);

        return BetsPage.builder()
                .bets(BetDto.from(allBets.getContent()))
                .totalPages(allBets.getTotalPages())
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto editBet(String moderatorId, String editedBetId, EditedBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());
        checkGameScore(editedBet.getGameScore(), Bet.BetStatus.valueOf(editedBet.getBetStatus()));

        User moderator = getEntityService.getUserOrThrow(moderatorId);
        User newUser = getEntityService.getUserOrThrow(editedBet.getUserId());
        Bet bet = getEntityService.getBetOrThrow(editedBetId);
        Bet prevBetState = getPreviousStateOfBet(bet); // сохраняем изначальное состояние ставки до редактирования
        Team newHomeTeam = getEntityService.getTeamOrThrow(editedBet.getHomeTeamId());
        Team newAwayTeam = getEntityService.getTeamOrThrow(editedBet.getAwayTeamId());

        String seasonId = editedBet.getSeasonId();
        String leagueId = editedBet.getLeagueId();

        updateEditedBetValues(betsRepository, bet, editedBet, moderator, newUser, newHomeTeam, newAwayTeam);
        calendarsService.updateCalendar(prevBetState, bet);
        betsRepository.save(bet);

        playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, newUser, bet, true);
        playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, prevBetState.getUser(), prevBetState, false);
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, newUser.getId(), bet, true);
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, prevBetState.getUser().getId(), prevBetState, false);
        gameweekStatsService.calculateGameweekStats(bet.getCalendarNodeId());
        betTitleStatsService.calculateStatsByBetTitle(seasonId, newUser.getId(), bet, true);
        betTitleStatsService.calculateStatsByBetTitle(seasonId, prevBetState.getUser().getId(), prevBetState, false);

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData) {
        Bet bet = getEntityService.getBetOrThrow(betId);
        User moderator = getEntityService.getUserOrThrow(moderatorId);
        Bet.BetStatus betStatus = bet.getBetStatus();
        String seasonId = deletedBetMetaData.getSeasonId();
        String leagueId = deletedBetMetaData.getLeagueId();
        User user = bet.getUser();

        if (Bet.BetStatus.OPENED == betStatus) {
            playerStatsService.calculateStatsBasedOnNewOpenedBet(seasonId, leagueId, user, false);
        }
        if (Bet.BetStatus.EMPTY == betStatus) {
            playerStatsService.calculateStatsBasedOnEmptyBet(seasonId, leagueId, user, bet.getBetSize(), false);
        }
        if (WRL_STATUSES.contains(betStatus)) {
            playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, user, bet, false);
            teamStatsService.calculateStatsByTeams(seasonId, leagueId, user.getId(), bet, false);
            betTitleStatsService.calculateStatsByBetTitle(seasonId, user.getId(), bet, false);
        }

        updateDeletedBetValues(bet, moderator);
        BetDto betDto = BetDto.from(bet);
        bet.setBetStatus(Bet.BetStatus.DELETED);

        calendarsService.deleteBetFromCalendar(bet, deletedBetMetaData.getCalendarNodeId());

        betsRepository.save(bet);

        return betDto;
    }
}



