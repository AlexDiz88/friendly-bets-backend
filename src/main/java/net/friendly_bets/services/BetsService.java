package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static net.friendly_bets.utils.BetUtils.*;
import static net.friendly_bets.utils.Constants.*;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetsService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    BetsRepository betsRepository;
    TeamsRepository teamsRepository;
    UsersRepository usersRepository;

    CalendarsService calendarsService;
    PlayerStatsService playerStatsService;
    TeamStatsService teamStatsService;
    GameweekStatsService gameweekStatsService;


    @Transactional
    public BetDto addOpenedBet(String moderatorId, NewBet newOpenedBet) {
        validateBet(newOpenedBet);
        checkIfBetAlreadyExists(betsRepository, newOpenedBet);

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newOpenedBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, newOpenedBet.getSeasonId());
        League league = getLeagueOrThrow(leaguesRepository, newOpenedBet.getLeagueId());
        Team homeTeam = getTeamOrThrow(teamsRepository, newOpenedBet.getHomeTeamId());
        Team awayTeam = getTeamOrThrow(teamsRepository, newOpenedBet.getAwayTeamId());

        Bet openedBet = createNewOpenedBet(newOpenedBet, moderator, user, season, league, homeTeam, awayTeam);

        betsRepository.save(openedBet);
        updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);
        calendarsService.addBetToCalendarNode(openedBet.getId(), newOpenedBet.getCalendarNodeId(), newOpenedBet.getLeagueId());
        playerStatsService.calculateStatsBasedOnNewOpenedBet(season.getId(), league.getId(), user, true);

        return BetDto.from(openedBet);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto addEmptyBet(String moderatorId, NewEmptyBet newEmptyBet) {
        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newEmptyBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, newEmptyBet.getSeasonId());
        League league = getLeagueOrThrow(leaguesRepository, newEmptyBet.getLeagueId());

        Bet emptyBet = createNewEmptyBet(newEmptyBet, moderator, user, season, league);

        betsRepository.save(emptyBet);
        updateLeagueCurrentMatchDay(leaguesRepository, betsRepository, season, league);
        calendarsService.addBetToCalendarNode(emptyBet.getId(), newEmptyBet.getCalendarNodeId(), newEmptyBet.getLeagueId());
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

        checkGameResult(betResult.getGameResult(), Bet.BetStatus.valueOf(betResult.getBetStatus()));

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        Bet bet = getBetOrThrow(betsRepository, betId);
        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            throw new ConflictException("betAlreadyProcessed");
        }

        processBetResultValues(moderator, bet, betResult);
        betsRepository.save(bet);

        playerStatsService.calculateStatsBasedOnBetResult(bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser(), bet, true);
        teamStatsService.calculateStatsByTeams(bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser().getId(), bet, true);
        gameweekStatsService.calculateGameweekStats(bet.getCalendarNodeId());

        return BetDto.from(bet);
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
    public BetDto editBet(String moderatorId, String betId, EditedBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());
        checkGameResult(editedBet.getGameResult(), Bet.BetStatus.valueOf(editedBet.getBetStatus()));

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User newUser = getUserOrThrow(usersRepository, editedBet.getUserId());
        Bet bet = getBetOrThrow(betsRepository, betId);
        Team newHomeTeam = getTeamOrThrow(teamsRepository, editedBet.getHomeTeamId());
        Team newAwayTeam = getTeamOrThrow(teamsRepository, editedBet.getAwayTeamId());

        String seasonId = editedBet.getSeasonId();
        String leagueId = editedBet.getLeagueId();
        Bet prevBet = getPreviousStateOfBet(bet);

        updateEditedBetValues(betsRepository, bet, editedBet, moderator, newUser, newHomeTeam, newAwayTeam);
        calendarsService.updateCalendar(bet, editedBet);
        betsRepository.save(bet);

        playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, newUser, bet, true);
        playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, prevBet.getUser(), prevBet, false);
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, newUser.getId(), bet, true);
        teamStatsService.calculateStatsByTeams(seasonId, leagueId, prevBet.getUser().getId(), prevBet, false);
        gameweekStatsService.calculateGameweekStats(bet.getCalendarNodeId());

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData) {
        Bet bet = getBetOrThrow(betsRepository, betId);
        User moderator = getUserOrThrow(usersRepository, moderatorId);
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
        }

        updateDeletedBetValues(bet, moderator);
        BetDto betDto = BetDto.from(bet);
        bet.setBetStatus(Bet.BetStatus.DELETED);

        calendarsService.deleteBetFromCalendar(bet, deletedBetMetaData.getCalendarNodeId());

        betsRepository.save(bet);

        return betDto;
    }
}



