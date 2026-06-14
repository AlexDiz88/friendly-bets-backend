package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.exceptions.ForbiddenException;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    LeagueMatchdayService leagueMatchdayService;
    FootballDataSyncService footballDataSyncService;
    GameResultRecordRepository gameResultRecordRepository;
    FootballDataMatchdaySupport matchdaySupport;

    @Transactional
    public BetDto addOpenedBet(AuthenticatedUser currentUser, NewBetDto newOpenedBet) {
        User createdBy = getEntityService.getUserOrThrow(currentUser.getUser().getId());
        User user = getEntityService.getUserOrThrow(newOpenedBet.getUserId());
        Season season = getEntityService.getSeasonOrThrow(newOpenedBet.getSeasonId());
        League league = getEntityService.getLeagueOrThrow(newOpenedBet.getLeagueId());
        leagueMatchdayService.validateMatchDayForLeague(league, newOpenedBet.getMatchDay());

        boolean moderatorAction = isModeratorOrAdmin(createdBy.getRole());
        if (!moderatorAction) {
            if (!createdBy.getId().equals(newOpenedBet.getUserId())) {
                throw new ForbiddenException("accessDenied");
            }
            if (!isSeasonParticipant(season, createdBy.getId())) {
                throw new ForbiddenException("notSeasonParticipant");
            }
        }

        validateBet(newOpenedBet);
        checkIfBetAlreadyExists(betsRepository, newOpenedBet);
        calendarsService.getLeagueMatchdayNode(
                newOpenedBet.getCalendarNodeId(),
                newOpenedBet.getLeagueId(),
                newOpenedBet.getMatchDay()
        );
        Team homeTeam = getEntityService.getTeamOrThrow(newOpenedBet.getHomeTeamId());
        Team awayTeam = getEntityService.getTeamOrThrow(newOpenedBet.getAwayTeamId());

        if (!moderatorAction) {
            validateMatchNotStartedForSelfBet(season, league, newOpenedBet, homeTeam.getId(), awayTeam.getId());
        }

        Bet openedBet = createNewOpenedBet(newOpenedBet, createdBy, user, season, league, homeTeam, awayTeam);

        betsRepository.save(openedBet);
        leagueMatchdayService.updateCurrentMatchDayAfterBet(season, league);
        calendarsService.addBetToCalendarNode(openedBet, newOpenedBet.getCalendarNodeId(), newOpenedBet.getLeagueId(), newOpenedBet.getMatchDay());
        playerStatsService.calculateStatsBasedOnNewOpenedBet(season.getId(), league.getId(), user, true);
        footballDataSyncService.registerPollingForOpenedBet(openedBet);

        return BetDto.from(openedBet);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public BetDto addEmptyBet(String moderatorId, NewEmptyBet newEmptyBet) {
        User moderator = getEntityService.getUserOrThrow(moderatorId);
        User user = getEntityService.getUserOrThrow(newEmptyBet.getUserId());
        Season season = getEntityService.getSeasonOrThrow(newEmptyBet.getSeasonId());
        League league = getEntityService.getLeagueOrThrow(newEmptyBet.getLeagueId());
        leagueMatchdayService.validateMatchDayForLeague(league, newEmptyBet.getMatchDay());

        calendarsService.getLeagueMatchdayNode(
                newEmptyBet.getCalendarNodeId(),
                newEmptyBet.getLeagueId(),
                newEmptyBet.getMatchDay()
        );

        Bet emptyBet = createNewEmptyBet(newEmptyBet, moderator, user, season, league);

        betsRepository.save(emptyBet);
        leagueMatchdayService.updateCurrentMatchDayAfterBet(season, league);
        calendarsService.addBetToCalendarNode(emptyBet, newEmptyBet.getCalendarNodeId(), newEmptyBet.getLeagueId(), newEmptyBet.getMatchDay());
        playerStatsService.calculateStatsBasedOnEmptyBet(season.getId(), league.getId(), user, newEmptyBet.getBetSize(), true);
        gameweekStatsService.calculateGameweekStats(newEmptyBet.getCalendarNodeId());

        return BetDto.from(emptyBet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Transactional
    public BetDto setBetResult(String moderatorId, String betId, BetResult betResult) {
        return setBetResult(moderatorId, betId, betResult, true);
    }

    @Transactional
    public BetDto setBetResult(String moderatorId, String betId, BetResult betResult, boolean updateGameweekStats) {
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
        if (updateGameweekStats) {
            gameweekStatsService.calculateGameweekStats(bet.getCalendarNodeId());
        }
        betTitleStatsService.calculateStatsByBetTitle(seasonId, userId, bet, true);

        return BetDto.from(bet);
    }

    @Transactional
    public BetsPage setBetResults(String moderatorId, String seasonId, List<GameResult> gameResults) {
        return setBetResults(moderatorId, seasonId, gameResults, true);
    }

    @Transactional
    public BetsPage setBetResults(
            String moderatorId,
            String seasonId,
            List<GameResult> gameResults,
            boolean updateGameweekAfterEachBet
    ) {
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

                setBetResult(moderatorId, bet.getId(), betResult, updateGameweekAfterEachBet);
                processedBets.add(getEntityService.getBetOrThrow(bet.getId()));
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


    public BetsPage getUserSlotBets(String seasonId, String userId, String leagueId, String matchDay) {
        if (leagueId == null || leagueId.isBlank() || matchDay == null || matchDay.isBlank()) {
            throw new BadRequestException("invalidRequest");
        }
        List<Bet> bets = betsRepository.findAllBySeason_IdAndUser_IdAndLeague_IdAndMatchDayAndBetStatusIn(
                seasonId,
                userId,
                leagueId,
                matchDay.trim(),
                VALID_BET_STATUSES
        );
        return BetsPage.builder()
                .bets(BetDto.from(bets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public BetsPage getMatchBets(
            AuthenticatedUser currentUser,
            String seasonId,
            String leagueId,
            String matchDay,
            String homeTeamId,
            String awayTeamId
    ) {
        if (leagueId == null || leagueId.isBlank()
                || matchDay == null || matchDay.isBlank()
                || homeTeamId == null || homeTeamId.isBlank()
                || awayTeamId == null || awayTeamId.isBlank()) {
            throw new BadRequestException("invalidRequest");
        }
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        String userId = currentUser.getUser().getId();
        if (!isSeasonParticipant(season, userId)) {
            throw new ForbiddenException("notSeasonParticipant");
        }
        List<Bet> bets = betsRepository.findAllBySeason_IdAndLeague_IdAndMatchDayAndHomeTeam_IdAndAwayTeam_IdAndBetStatusIn(
                seasonId,
                leagueId,
                matchDay.trim(),
                homeTeamId.trim(),
                awayTeamId.trim(),
                MATCH_BET_STATUSES
        );
        List<Bet> placedBets = bets.stream()
                .filter(bet -> bet.getBetTitle() != null)
                .sorted((a, b) -> {
                    String nameA = a.getUser() != null && a.getUser().getUsername() != null
                            ? a.getUser().getUsername()
                            : "";
                    String nameB = b.getUser() != null && b.getUser().getUsername() != null
                            ? b.getUser().getUsername()
                            : "";
                    int byName = nameA.compareToIgnoreCase(nameB);
                    if (byName != 0) {
                        return byName;
                    }
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .collect(Collectors.toList());
        return BetsPage.builder()
                .bets(BetDto.from(placedBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public SlotMatchBetCountsDto getSlotMatchBetCounts(
            AuthenticatedUser currentUser,
            String seasonId,
            String leagueId,
            String matchDay
    ) {
        if (leagueId == null || leagueId.isBlank() || matchDay == null || matchDay.isBlank()) {
            throw new BadRequestException("invalidRequest");
        }
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        String userId = currentUser.getUser().getId();
        if (!isSeasonParticipant(season, userId)) {
            throw new ForbiddenException("notSeasonParticipant");
        }
        List<Bet> bets = betsRepository.findAllBySeason_IdAndLeague_IdAndMatchDayAndBetStatusIn(
                seasonId,
                leagueId,
                matchDay.trim(),
                MATCH_BET_STATUSES
        );
        Map<String, Integer> counts = new HashMap<>();
        for (Bet bet : bets) {
            if (bet.getBetTitle() == null || bet.getHomeTeam() == null || bet.getAwayTeam() == null) {
                continue;
            }
            String homeTeamId = bet.getHomeTeam().getId();
            String awayTeamId = bet.getAwayTeam().getId();
            if (homeTeamId == null || homeTeamId.isBlank() || awayTeamId == null || awayTeamId.isBlank()) {
                continue;
            }
            String key = homeTeamId + "_" + awayTeamId;
            counts.merge(key, 1, Integer::sum);
        }
        return SlotMatchBetCountsDto.builder()
                .counts(counts)
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
        League league = getEntityService.getLeagueOrThrow(editedBet.getLeagueId());
        leagueMatchdayService.validateMatchDayForLeague(league, editedBet.getMatchDay());
        validateEditedBetStatusTransition(bet.getBetStatus(), Bet.BetStatus.valueOf(editedBet.getBetStatus()));
        Bet prevBetState = getPreviousStateOfBet(bet); // сохраняем изначальное состояние ставки до редактирования
        Team newHomeTeam = getEntityService.getTeamOrThrow(editedBet.getHomeTeamId());
        Team newAwayTeam = getEntityService.getTeamOrThrow(editedBet.getAwayTeamId());

        String seasonId = editedBet.getSeasonId();
        String leagueId = editedBet.getLeagueId();

        updateEditedBetValues(betsRepository, bet, editedBet, moderator, newUser, newHomeTeam, newAwayTeam);
        calendarsService.updateCalendar(prevBetState, bet);
        betsRepository.save(bet);

        revertStatsForEditedBet(prevBetState);
        applyStatsForEditedBet(bet, seasonId, leagueId, newUser);
        String prevCalendarNodeId = prevBetState.getCalendarNodeId();
        String newCalendarNodeId = bet.getCalendarNodeId();
        gameweekStatsService.calculateGameweekStats(newCalendarNodeId);
        if (prevCalendarNodeId != null && !Objects.equals(prevCalendarNodeId, newCalendarNodeId)) {
            gameweekStatsService.calculateGameweekStats(prevCalendarNodeId);
        }

        return BetDto.from(bet);
    }

    private void revertStatsForEditedBet(Bet prevBetState) {
        String prevSeasonId = prevBetState.getSeason().getId();
        String prevLeagueId = prevBetState.getLeague().getId();
        User prevUser = prevBetState.getUser();

        playerStatsService.calculateStatsBasedOnEditedBet(prevSeasonId, prevLeagueId, prevUser, prevBetState, false);
        if (WRL_STATUSES.contains(prevBetState.getBetStatus())) {
            teamStatsService.calculateStatsByTeams(prevSeasonId, prevLeagueId, prevUser.getId(), prevBetState, false);
            betTitleStatsService.calculateStatsByBetTitle(prevSeasonId, prevUser.getId(), prevBetState, false);
        }
    }

    private void applyStatsForEditedBet(Bet bet, String seasonId, String leagueId, User user) {
        playerStatsService.calculateStatsBasedOnEditedBet(seasonId, leagueId, user, bet, true);
        if (WRL_STATUSES.contains(bet.getBetStatus())) {
            teamStatsService.calculateStatsByTeams(seasonId, leagueId, user.getId(), bet, true);
            betTitleStatsService.calculateStatsByBetTitle(seasonId, user.getId(), bet, true);
        }
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
        bet.setCalendarNodeId(null);

        calendarsService.deleteBetFromCalendar(bet, deletedBetMetaData.getCalendarNodeId());

        betsRepository.save(bet);

        String calendarNodeId = deletedBetMetaData.getCalendarNodeId();
        if (calendarNodeId != null && !calendarNodeId.isBlank()) {
            gameweekStatsService.calculateGameweekStats(calendarNodeId);
        }

        return betDto;
    }

    private static boolean isModeratorOrAdmin(User.Role role) {
        return role == User.Role.MODERATOR || role == User.Role.ADMIN;
    }

    private static boolean isSeasonParticipant(Season season, String userId) {
        if (userId == null || season.getPlayers() == null) {
            return false;
        }
        return season.getPlayers().stream()
                .filter(Objects::nonNull)
                .map(User::getId)
                .anyMatch(userId::equals);
    }

    private void validateMatchNotStartedForSelfBet(
            Season season,
            League league,
            NewBetDto newBet,
            String homeTeamId,
            String awayTeamId
    ) {
        if (league.getLeagueCode() == null) {
            throw new BadRequestException("gameResultNotFound");
        }
        String storageSeason = matchdaySupport.resolveFootballDataSeasonYear(season, league.getLeagueCode());
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndSeasonAndHomeTeamIdAndAwayTeamId(
                league.getLeagueCode().name(),
                storageSeason,
                homeTeamId,
                awayTeamId
        );
        if (matches.isEmpty()) {
            throw new BadRequestException("gameResultNotFound");
        }
        Optional<Integer> slotOrder = matchdaySupport.resolveSlotOrder(league, newBet.getMatchDay());
        GameResultRecord match = matches.stream()
                .filter(m -> slotOrder.isEmpty() || Objects.equals(m.getMatchday(), slotOrder.get()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("gameResultNotFound"));
        if (!GameResultNotStarted.isNotStarted(match)) {
            throw new BadRequestException("matchAlreadyStarted");
        }
    }
}



