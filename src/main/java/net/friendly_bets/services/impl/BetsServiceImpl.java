package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.BetsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.friendly_bets.services.impl.PlayerStatsServiceImpl.calculateTeamsStats;
import static net.friendly_bets.utils.BetValuesUtils.*;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetsServiceImpl implements BetsService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    BetsRepository betsRepository;
    TeamsRepository teamsRepository;
    UsersRepository usersRepository;
    PlayerStatsRepository playerStatsRepository;
    PlayerStatsByTeamsRepository playerStatsByTeamsRepository;
    MongoOperations mongoOperations;

    @Override
    @Transactional
    public BetDto addBet(String moderatorId, NewBetDto newBet) {
        checkTeams(newBet.getHomeTeamId(), newBet.getAwayTeamId());
        checkBetOdds(newBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, newBet.getSeasonId());
        League league = getLeagueOrThrow(leaguesRepository, newBet.getLeagueId());
        Team homeTeam = getTeamOrThrow(teamsRepository, newBet.getHomeTeamId());
        Team awayTeam = getTeamOrThrow(teamsRepository, newBet.getAwayTeamId());

        // TODO написать проверку совпадения ставок под лигу, а не из общего репозитория ставок
        //  (т.к. могут быть совпадения в разных лигах/сезонах)
        if (betsRepository.existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
                user,
                newBet.getMatchDay(),
                homeTeam,
                awayTeam,
                newBet.getBetTitle(),
                newBet.getBetOdds(),
                newBet.getBetSize()
        )) {
            throw new ConflictException("Ставка на этот матч от данного участника уже добавлена");
        }

        Bet bet = Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(user)
                .season(season)
                .league(league)
                .matchDay(newBet.getMatchDay())
                .gameId(newBet.getGameId())
                .gameDate(newBet.getGameDate())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .betTitle(newBet.getBetTitle())
                .betOdds(newBet.getBetOdds())
                .betSize(newBet.getBetSize())
                .betStatus(Bet.BetStatus.OPENED)
                .build();

        betsRepository.save(bet);
        setCurrentMatchDay(betsRepository, season, league);
        leaguesRepository.save(league);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(newBet.getSeasonId(), newBet.getLeagueId(), user);
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(newBet.getSeasonId(), newBet.getLeagueId(), user));

        playerStats.setTotalBets(playerStats.getTotalBets() + 1);
        playerStatsRepository.save(playerStats);

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto addEmptyBet(String moderatorId, NewEmptyBetDto newEmptyBet) {
        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newEmptyBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, newEmptyBet.getSeasonId());
        League league = getLeagueOrThrow(leaguesRepository, newEmptyBet.getLeagueId());

        Bet bet = Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(user)
                .season(season)
                .league(league)
                .matchDay(newEmptyBet.getMatchDay())
                .betSize(newEmptyBet.getBetSize())
                .betStatus(Bet.BetStatus.EMPTY)
                .betResultAddedAt(LocalDateTime.now())
                .betResultAddedBy(moderator)
                .balanceChange(-Double.valueOf(newEmptyBet.getBetSize()))
                .build();

        betsRepository.save(bet);
        setCurrentMatchDay(betsRepository, season, league);
        leaguesRepository.save(league);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(newEmptyBet.getSeasonId(), newEmptyBet.getLeagueId(), user);
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(newEmptyBet.getSeasonId(), newEmptyBet.getLeagueId(), user));

        playerStats.setTotalBets(playerStats.getTotalBets() + 1);
        playerStats.setBetCount(playerStats.getBetCount() + 1);
        playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + 1);
        playerStats.setActualBalance(playerStats.getActualBalance() - Double.valueOf(newEmptyBet.getBetSize()));
        playerStatsRepository.save(playerStats);

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto setBetResult(String moderatorId, String betId, NewBetResult newBetResult) {
        try {
            Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый статус: " + newBetResult.getBetStatus());
        }

        checkGameResult(newBetResult.getGameResult());

        Bet bet = getBetOrThrow(betsRepository, betId);
        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            throw new ConflictException("Эта ставка уже обработана другим модератором");
        }

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        Bet.BetStatus betStatus = Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        setBalanceChange(bet, betStatus, bet.getBetSize(), bet.getBetOdds());

        bet.setBetResultAddedAt(LocalDateTime.now());
        bet.setBetResultAddedBy(moderator);
        bet.setBetStatus(betStatus);
        bet.setGameResult(newBetResult.getGameResult());
        betsRepository.save(bet);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser());
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser()));
        Optional<PlayerStatsByTeams> playerStatsByTeamsOptional = playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndUserAndIsLeagueStats(bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser(), false);
        PlayerStatsByTeams playerStatsByTeams = playerStatsByTeamsOptional.orElseGet(() -> getDefaultStatsByTeams(bet.getSeason().getId(), bet.getLeague().getId(), bet.getLeague().getDisplayNameRu(), bet.getUser(), false));
        Optional<PlayerStatsByTeams> leagueStatsByTeamsOptional = playerStatsByTeamsRepository.findBySeasonIdAndLeagueIdAndIsLeagueStats(bet.getSeason().getId(), bet.getLeague().getId(), true);
        PlayerStatsByTeams leagueStatsByTeams = leagueStatsByTeamsOptional.orElseGet(() -> getDefaultStatsByTeams(bet.getSeason().getId(), bet.getLeague().getId(), bet.getLeague().getDisplayNameRu(), bet.getUser(), true));

        playerStats.setBetCount(playerStats.getBetCount() + 1);
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
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() + bet.getBetOdds());
        playerStats.setActualBalance(playerStats.getActualBalance() + bet.getBalanceChange());
        recalculatePlayerStats(playerStats);
        playerStatsRepository.save(playerStats);

        PlayerStatsByTeams updatedStatsByTeams = calculateTeamsStats(bet, playerStatsByTeams);
        PlayerStatsByTeams updatedLeagueStatsByTeams = calculateTeamsStats(bet, leagueStatsByTeams);
        playerStatsByTeamsRepository.saveAll(List.of(updatedStatsByTeams, updatedLeagueStatsByTeams));

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getOpenedBets(String seasonId) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(seasonId, Bet.BetStatus.OPENED);
        return BetsPage.builder()
                .bets(BetDto.from(openedBets))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getCompletedBets(String seasonId, String playerId, String leagueId, Pageable pageable) {
        System.out.println("getPageSize:" + pageable.getPageSize());
        System.out.println("getPageNumber:" + pageable.getPageNumber());
        System.out.println("playerId:" + playerId);
        System.out.println("leagueId:" + leagueId);

//        Page<Bet> betsPage = betsRepository.findAllByBetStatusIn(desiredStatuses, pageable);

//        mongoOperations.indexOps(Bet.class).ensureIndex(new Index().on("user", Sort.Direction.ASC));
//        mongoOperations.indexOps(Bet.class).ensureIndex(new Index().on("league", Sort.Direction.ASC));
//        mongoOperations.indexOps(Bet.class).ensureIndex(new Index().on("betStatus", Sort.Direction.ASC));
//        mongoOperations.indexOps(Bet.class).ensureIndex(new Index().on("createdAt", Sort.Direction.DESC));
//        mongoOperations.indexOps(Bet.class).ensureIndex(new Index().on("betResultAddedAt", Sort.Direction.DESC));

        List<Bet.BetStatus> desiredStatuses = List.of(Bet.BetStatus.WON, Bet.BetStatus.RETURNED,
                Bet.BetStatus.LOST, Bet.BetStatus.EMPTY);

        Page<Bet> completedBetsPage = null;

        if (leagueId == null && playerId == null) {
            completedBetsPage = betsRepository.findAllByBetStatusIn(desiredStatuses, pageable);
        }
        if (leagueId == null && playerId != null) {
            completedBetsPage = betsRepository.findAllByBetStatusInAndUser_Id(desiredStatuses, playerId, pageable);
        }
        if (leagueId != null && playerId == null) {
            completedBetsPage = betsRepository.findAllByBetStatusInAndLeague_Id(desiredStatuses, leagueId, pageable);
        }
        if (leagueId != null && playerId != null) {
            completedBetsPage = betsRepository.findAllByBetStatusInAndLeague_IdAndUser_Id(desiredStatuses, leagueId, playerId, pageable);
        }
        if (completedBetsPage == null) {
            throw new BadRequestException("Некорректный запрос");
        }

//        Criteria criteria = new Criteria();
//        // TODO: Добавить фильтр по сезону
//        Optional<League> leagueOptional = leagueName != null ? leaguesRepository.findByDisplayNameRu(leagueName) : Optional.empty();
//        leagueOptional.ifPresent(league -> criteria.and("league").is(league.getId()));
//        Optional<User> userOptional = playerName != null ? usersRepository.findByUsername(playerName) : Optional.empty();
//        userOptional.ifPresent(user -> criteria.and("user").is(user.getId()));
//        criteria.and("betStatus").in(desiredStatuses);

//        Query query = new Query(criteria).with(pageable);
//        List<Bet> completedBets = mongoOperations.find(query, Bet.class);
//        Page<Bet> page = new PageImpl<>(completedBets, pageable, completedBets.size());
//        int totalPages = page.getTotalPages();
//        int totalAmountBets = (int) page.getTotalElements();

        return BetsPage.builder()
                .bets(BetDto.from(completedBetsPage.getContent()))
                .totalPages(completedBetsPage.getTotalPages())
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getAllBets(String seasonId, Pageable pageable) {
        List<Bet.BetStatus> desiredStatuses = List.of(Bet.BetStatus.OPENED, Bet.BetStatus.WON, Bet.BetStatus.RETURNED,
                Bet.BetStatus.LOST, Bet.BetStatus.EMPTY);

        Page<Bet> allBets = betsRepository.findAllByBetStatusIn(desiredStatuses, pageable);

//        Criteria criteria = new Criteria();
//        // TODO: Добавить фильтр по сезону
//        criteria.and("betStatus").in(desiredStatuses);
//
//        Query query = new Query(criteria).with(pageable);
//        List<Bet> allBets = mongoOperations.find(query, Bet.class);
//        Page<Bet> page = new PageImpl<>(allBets, pageable, allBets.size());


        return BetsPage.builder()
                .bets(BetDto.from(allBets.getContent()))
                .totalPages(allBets.getTotalPages())
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
            recalculatePlayerStats(playerStats);
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
            recalculatePlayerStats(previousPlayerStats);
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
            recalculatePlayerStats(playerStats);
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

        return BetDto.from(bet);
    }

    // ------------------------------------------------------------------------------------------------------ //
    // группа методов для РЕДАКТИРОВАНИЯ ставки и пересчёта всей связанной статистики

    //    @Override
    @Transactional
    public BetDto editBet2(String moderatorId, String betId, EditedCompleteBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, editedBet.getUserId());
        Team homeTeam = getTeamOrThrow(teamsRepository, editedBet.getHomeTeamId());
        Team awayTeam = getTeamOrThrow(teamsRepository, editedBet.getAwayTeamId());
        Bet bet = getBetOrThrow(betsRepository, betId);

        Bet previousBet = createPreviousBet(bet);

        handleBetStatus(bet, editedBet, user, homeTeam, awayTeam);
        updateBetDetails(bet, moderator, user, editedBet, homeTeam, awayTeam);

        updatePlayerStats(user, previousBet, bet, editedBet);

        return BetDto.from(bet);
    }

    private Bet createPreviousBet(Bet bet) {
        return Bet.builder()
                .user(bet.getUser())
                .betOdds(bet.getBetOdds())
                .betStatus(bet.getBetStatus())
                .balanceChange(bet.getBalanceChange())
                .betSize(bet.getBetSize())
                .gameResult(bet.getGameResult())
                .build();
    }

    private void handleBetStatus(Bet bet, EditedCompleteBetDto editedBet, User user, Team homeTeam, Team awayTeam) {
        Bet.BetStatus newStatus = Bet.BetStatus.valueOf(editedBet.getBetStatus());

        if (bet.getBetStatus() != Bet.BetStatus.OPENED) {
            validateGameResult(editedBet);
            validateBetUniqueness(user, editedBet, homeTeam, awayTeam, newStatus);
            setBalanceChange(bet, newStatus, editedBet.getBetSize(), editedBet.getBetOdds());
            bet.setGameResult(editedBet.getGameResult());
        } else {
            validateBetUniqueness(user, editedBet, homeTeam, awayTeam, newStatus);
        }

        bet.setBetStatus(newStatus);
    }

    private void validateGameResult(EditedCompleteBetDto editedBet) {
        if (StringUtils.isBlank(editedBet.getGameResult())) {
            throw new BadRequestException("Счёт матча отсутствует");
        }
        checkGameResult(editedBet.getGameResult());
    }

    private void validateBetUniqueness(User user, EditedCompleteBetDto editedBet, Team homeTeam, Team awayTeam, Bet.BetStatus newStatus) {
        if (betsRepository.existsByUserAndMatchDayAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                user, editedBet.getMatchDay(), homeTeam, awayTeam, editedBet.getBetTitle(),
                editedBet.getBetOdds(), editedBet.getBetSize(), editedBet.getGameResult(), newStatus)) {
            throw new ConflictException("Ставка на этот матч уже отредактирована другим модератором");
        }
    }

    private void updateBetDetails(Bet bet, User moderator, User user, EditedCompleteBetDto editedBet, Team homeTeam, Team awayTeam) {
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
    }

    private void updatePlayerStats(User user, Bet previousBet, Bet bet, EditedCompleteBetDto editedBet) {
        if (user.equals(previousBet.getUser()) && !bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(editedBet.getSeasonId(), editedBet.getLeagueId(), user);
            PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(editedBet.getSeasonId(), editedBet.getLeagueId(), user));

            updatePlayerStatsOnBetStatusChange(playerStats, previousBet, bet);

            playerStats.setActualBalance(playerStats.getActualBalance() - previousBet.getBalanceChange() + bet.getBalanceChange());
            recalculatePlayerStats(playerStats);
            playerStatsRepository.save(playerStats);
        }
    }

    private void updatePlayerStatsOnBetStatusChange(PlayerStats playerStats, Bet previousBet, Bet currentBet) {
        if (previousBet.getBetStatus() != currentBet.getBetStatus()) {
            if (!previousBet.getBetOdds().equals(currentBet.getBetOdds())) {
                playerStats.setSumOfOdds(playerStats.getSumOfOdds() - previousBet.getBetOdds() + currentBet.getBetOdds());

                if (currentBet.getBetStatus().equals(Bet.BetStatus.WON)) {
                    playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - previousBet.getBetOdds() + currentBet.getBetOdds());
                    playerStats.calculateAverageWonBetOdds();
                }
            }

            if (previousBet.getBetStatus().equals(Bet.BetStatus.WON)) {
                playerStats.setWonBetCount(playerStats.getWonBetCount() - 1);
                playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - previousBet.getBetOdds());
            } else if (previousBet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() - 1);
            } else if (previousBet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                playerStats.setLostBetCount(playerStats.getLostBetCount() - 1);
            }

            if (currentBet.getBetStatus().equals(Bet.BetStatus.WON)) {
                playerStats.setWonBetCount(playerStats.getWonBetCount() + 1);
                playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + currentBet.getBetOdds());
            } else if (currentBet.getBetStatus().equals(Bet.BetStatus.RETURNED)) {
                playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + 1);
            } else if (currentBet.getBetStatus().equals(Bet.BetStatus.LOST)) {
                playerStats.setLostBetCount(playerStats.getLostBetCount() + 1);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ //
    // группа методов для УДАЛЕНИЯ ставки и пересчёта всей связанной статистики

    @Override
    @Transactional
    public BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData) {
        Bet bet = getBetOrThrow(betsRepository, betId);
        User moderator = getUserOrThrow(usersRepository, moderatorId);
        String homeTeamId = bet.getHomeTeam().getId();
        String awayTeamId = bet.getAwayTeam().getId();

        PlayerStats playerStats = getPlayerStatsOrThrow(playerStatsRepository, deletedBetMetaData.getSeasonId(), deletedBetMetaData.getLeagueId(), bet.getUser());
        PlayerStatsByTeams playerStatsByTeams = getPlayerStatsByTeamsOrThrow(playerStatsByTeamsRepository, bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser(), false);
        PlayerStatsByTeams leagueStatsByTeams = getLeagueStatsByTeamsOrThrow(playerStatsByTeamsRepository, bet.getSeason().getId(), bet.getLeague().getId(), true);

        processStats(bet, homeTeamId, awayTeamId, playerStats, playerStatsByTeams, leagueStatsByTeams);

        recalculatePlayerStats(playerStats);
        playerStatsRepository.save(playerStats);
        playerStatsByTeamsRepository.saveAll(List.of(playerStatsByTeams, leagueStatsByTeams));

        if (bet.getBetStatus() != Bet.BetStatus.OPENED && bet.getBetStatus() != Bet.BetStatus.DELETED) {
            bet.setBalanceChange(0.0);
        }

        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        BetDto betDto = BetDto.from(bet);
        bet.setBetStatus(Bet.BetStatus.DELETED);

        betsRepository.save(bet);

        return betDto;
    }

    private void processStats(Bet bet, String homeTeamId, String awayTeamId, PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams) {
        playerStats.setTotalBets(playerStats.getTotalBets() - 1);

        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            playerStats.setBetCount(playerStats.getBetCount() - 1);
            playerStats.setActualBalance(playerStats.getActualBalance() - bet.getBalanceChange());

            processTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
            processTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }

        switch (bet.getBetStatus()) {
            case EMPTY -> playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() - 1);
            case WON ->
                    processWonStats(playerStats, playerStatsByTeams, leagueStatsByTeams, homeTeamId, awayTeamId, bet);
            case RETURNED ->
                    processReturnedStats(playerStats, playerStatsByTeams, leagueStatsByTeams, homeTeamId, awayTeamId, bet);
            case LOST ->
                    processLostStats(playerStats, playerStatsByTeams, leagueStatsByTeams, homeTeamId, awayTeamId, bet);
        }
    }

    private void processTeamStats(List<TeamStats> teamStatsList, String homeTeamId, String awayTeamId, Bet bet) {
        for (TeamStats teamStats : teamStatsList) {
            String teamId = teamStats.getTeam().getId();
            if (teamId.equals(homeTeamId) || teamId.equals(awayTeamId)) {
                teamStats.setBetCount(teamStats.getBetCount() - 1);
                teamStats.setActualBalance(teamStats.getActualBalance() - bet.getBalanceChange());
            }
        }
    }

    private void processWonStats(PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams, String homeTeamId, String awayTeamId, Bet bet) {
        playerStats.setWonBetCount(playerStats.getWonBetCount() - 1);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());
        playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() - bet.getBetOdds());

        processWonTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        processWonTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
    }

    private void processReturnedStats(PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams, String homeTeamId, String awayTeamId, Bet bet) {
        playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() - 1);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());

        processReturnedTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        processReturnedTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
    }

    private void processLostStats(PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams, String homeTeamId, String awayTeamId, Bet bet) {
        playerStats.setLostBetCount(playerStats.getLostBetCount() - 1);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());

        processLostTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        processLostTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
    }

    private void processWonTeamStats(List<TeamStats> teamStatsList, String homeTeamId, String awayTeamId, Bet bet) {
        for (TeamStats teamStats : teamStatsList) {
            String teamId = teamStats.getTeam().getId();
            if (teamId.equals(homeTeamId) || teamId.equals(awayTeamId)) {
                teamStats.setWonBetCount(teamStats.getWonBetCount() - 1);
                teamStats.setSumOfOdds(teamStats.getSumOfOdds() - bet.getBetOdds());
                teamStats.setSumOfWonOdds(teamStats.getSumOfWonOdds() - bet.getBetOdds());
                recalculateStatsByTeams(teamStats);
            }
        }
    }

    private void processReturnedTeamStats(List<TeamStats> teamStatsList, String homeTeamId, String awayTeamId, Bet bet) {
        for (TeamStats teamStats : teamStatsList) {
            String teamId = teamStats.getTeam().getId();
            if (teamId.equals(homeTeamId) || teamId.equals(awayTeamId)) {
                teamStats.setReturnedBetCount(teamStats.getReturnedBetCount() - 1);
                teamStats.setSumOfOdds(teamStats.getSumOfOdds() - bet.getBetOdds());
                recalculateStatsByTeams(teamStats);
            }
        }
    }

    private void processLostTeamStats(List<TeamStats> teamStatsList, String homeTeamId, String awayTeamId, Bet bet) {
        for (TeamStats teamStats : teamStatsList) {
            String teamId = teamStats.getTeam().getId();
            if (teamId.equals(homeTeamId) || teamId.equals(awayTeamId)) {
                teamStats.setLostBetCount(teamStats.getLostBetCount() - 1);
                teamStats.setSumOfOdds(teamStats.getSumOfOdds() - bet.getBetOdds());
                recalculateStatsByTeams(teamStats);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ //

    private static void recalculatePlayerStats(PlayerStats stats) {
        stats.calculateWinRate();
        stats.calculateAverageOdds();
        stats.calculateAverageWonBetOdds();
    }

    private static void recalculateStatsByTeams(TeamStats stats) {
        stats.calculateWinRate();
        stats.calculateAverageOdds();
        stats.calculateAverageWonBetOdds();
    }

    // ------------------------------------------------------------------------------------------------------ //

}



