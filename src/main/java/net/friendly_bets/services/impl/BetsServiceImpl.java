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

        // TODO: есть метод для доп.проверки по сезону и лиге. Необходимо протестировать перед замено
        //  (замена нужна т.к. могут быть совпадения в разных лигах/сезонах)
        if (betsRepository.existsByUserAndMatchDayAndPlayoffRoundAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSize(
                user,
                newBet.getMatchDay(),
                newBet.getPlayoffRound(),
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
                .isPlayoff(newBet.getIsPlayoff())
                .matchDay(newBet.getMatchDay())
                .playoffRound(newBet.getPlayoffRound())
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
                .isPlayoff(newEmptyBet.getIsPlayoff())
                .matchDay(newEmptyBet.getMatchDay())
                .playoffRound(newEmptyBet.getPlayoffRound())
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
        playerStatsByTeamsRepository.save(updatedStatsByTeams);
        PlayerStatsByTeams updatedLeagueStatsByTeams = calculateTeamsStats(bet, leagueStatsByTeams);
        playerStatsByTeamsRepository.save(updatedLeagueStatsByTeams);

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
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusIn(seasonId, desiredStatuses, pageable);
        }
        if (leagueId == null && playerId != null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndUser_Id(seasonId, desiredStatuses, playerId, pageable);
        }
        if (leagueId != null && playerId == null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndLeague_Id(seasonId, desiredStatuses, leagueId, pageable);
        }
        if (leagueId != null && playerId != null) {
            completedBetsPage = betsRepository.findAllBySeason_IdAndBetStatusInAndLeague_IdAndUser_Id(seasonId, desiredStatuses, leagueId, playerId, pageable);
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

        Page<Bet> allBets = betsRepository.findAllBySeason_IdAndBetStatusIn(seasonId, desiredStatuses, pageable);

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
    // группа методов для РЕДАКТИРОВАНИЯ ставки и пересчёта всей связанной статистики

    @Override
    @Transactional
    public BetDto editBet(String moderatorId, String betId, EditedCompleteBetDto editedBet) {
        checkTeams(editedBet.getHomeTeamId(), editedBet.getAwayTeamId());
        checkBetOdds(editedBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User newUser = getUserOrThrow(usersRepository, editedBet.getUserId());
        Bet betInDB = getBetOrThrow(betsRepository, betId);
        Team newHomeTeam = getTeamOrThrow(teamsRepository, editedBet.getHomeTeamId());
        Team newAwayTeam = getTeamOrThrow(teamsRepository, editedBet.getAwayTeamId());

        Bet previousStateOfBet = getPreviousStateOfBet(betInDB);

        handleBetStatus(betInDB, editedBet, newUser, newHomeTeam, newAwayTeam);
        updateBetDetails(betInDB, moderator, newUser, editedBet, newHomeTeam, newAwayTeam);
        updatePlayerStats(betInDB, previousStateOfBet, editedBet);

        return BetDto.from(betInDB);
    }

    private Bet getPreviousStateOfBet(Bet bet) {
        if (bet.getBetStatus().equals(Bet.BetStatus.EMPTY) || bet.getBetStatus().equals(Bet.BetStatus.DELETED)) {
            throw new BadRequestException("Пустые и удалённые ставки редактировать запрещено");
        }
        return Bet.builder()
                .user(bet.getUser())
                .isPlayoff(bet.getIsPlayoff())
                .matchDay(bet.getMatchDay())
                .playoffRound(bet.getPlayoffRound())
                .homeTeam(bet.getHomeTeam())
                .awayTeam(bet.getAwayTeam())
                .betTitle(bet.getBetTitle())
                .betOdds(bet.getBetOdds())
                .betSize(bet.getBetSize())
                .betStatus(bet.getBetStatus())
                .gameResult(bet.getGameResult())
                .balanceChange(bet.getBalanceChange())
                .build();
    }

    private void handleBetStatus(Bet bet, EditedCompleteBetDto editedBet, User user, Team homeTeam, Team awayTeam) {
        Bet.BetStatus newStatus = Bet.BetStatus.valueOf(editedBet.getBetStatus());
        validateBetUniqueness(user, editedBet, homeTeam, awayTeam, newStatus);

        if (bet.getBetStatus() != Bet.BetStatus.OPENED) {
            validateGameResult(editedBet);
            setBalanceChange(bet, newStatus, editedBet.getBetSize(), editedBet.getBetOdds());
            bet.setGameResult(editedBet.getGameResult());
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
        if (betsRepository.existsByUserAndMatchDayAndPlayoffRoundAndHomeTeamAndAwayTeamAndBetTitleAndBetOddsAndBetSizeAndGameResultAndBetStatus(
                user, editedBet.getMatchDay(), editedBet.getPlayoffRound(), homeTeam, awayTeam, editedBet.getBetTitle(),
                editedBet.getBetOdds(), editedBet.getBetSize(), editedBet.getGameResult(), newStatus)) {
            throw new ConflictException("Ставка на этот матч уже отредактирована другим модератором");
        }
    }

    @Transactional
    private void updateBetDetails(Bet bet, User moderator, User newUser, EditedCompleteBetDto editedBet, Team newHomeTeam, Team newAwayTeam) {
        bet.setUpdatedAt(LocalDateTime.now());
        bet.setUpdatedBy(moderator);
        bet.setUser(newUser);
        bet.setIsPlayoff(editedBet.getIsPlayoff());
        bet.setMatchDay(editedBet.getMatchDay());
        bet.setPlayoffRound(editedBet.getPlayoffRound());
        bet.setHomeTeam(newHomeTeam);
        bet.setAwayTeam(newAwayTeam);
        bet.setBetTitle(editedBet.getBetTitle());
        bet.setBetOdds(editedBet.getBetOdds());
        bet.setBetSize(editedBet.getBetSize());

        betsRepository.save(bet);
    }

    @Transactional
    private void updatePlayerStats(Bet betInDB, Bet previousStateOfBet, EditedCompleteBetDto editedBet) {
        PlayerStats statsOfPreviousPlayer = getPlayerStatsOrThrow(playerStatsRepository, editedBet.getSeasonId(), editedBet.getLeagueId(), previousStateOfBet.getUser());
        PlayerStats statsOfActualPlayer = getPlayerStatsOrThrow(playerStatsRepository, betInDB.getSeason().getId(), betInDB.getLeague().getId(), betInDB.getUser());
        PlayerStatsByTeams statsByTeamsOfPreviousPlayer = getPlayerStatsByTeamsOrNull(playerStatsByTeamsRepository, editedBet.getSeasonId(), editedBet.getLeagueId(), previousStateOfBet.getUser(), false);
        PlayerStatsByTeams statsByTeamsOfActualPlayer = getPlayerStatsByTeamsOrNull(playerStatsByTeamsRepository, editedBet.getSeasonId(), editedBet.getLeagueId(), betInDB.getUser(), false);
        PlayerStatsByTeams leagueStatsByTeams = getLeagueStatsByTeamsOrNull(playerStatsByTeamsRepository, editedBet.getSeasonId(), editedBet.getLeagueId(), true);

        // если статус OPENED и новый игрок -> обновление статистики для КАЖДОГО игрока (общая + по лиге). По командам менять НЕ нужно
        if (betInDB.getBetStatus() == Bet.BetStatus.OPENED && !editedBet.getUserId().equals(previousStateOfBet.getUser().getId())) {
            statsOfPreviousPlayer.setTotalBets(statsOfPreviousPlayer.getTotalBets() - 1);
            statsOfActualPlayer.setTotalBets(statsOfActualPlayer.getTotalBets() + 1);
        }

        boolean samePlayers = editedBet.getUserId().equals(previousStateOfBet.getUser().getId());
        // если статус WON/RETURNED/LOST и ТОТ ЖЕ игрок -> обновление статистики этого игрока (общая + по лиге)
        if (betInDB.getBetStatus() != Bet.BetStatus.OPENED && samePlayers) {
            updatePlayerStatsOnCompletedBet(statsOfActualPlayer, previousStateOfBet, false);
            updatePlayerStatsOnCompletedBet(statsOfActualPlayer, betInDB, true);
            updateTeamsStatsOnCompletedBet(statsByTeamsOfActualPlayer, previousStateOfBet, false);
            updateTeamsStatsOnCompletedBet(statsByTeamsOfActualPlayer, betInDB, true);
            updateTeamsStatsOnCompletedBet(leagueStatsByTeams, previousStateOfBet, false);
            updateTeamsStatsOnCompletedBet(leagueStatsByTeams, betInDB, true);
        }

        // если статус WON/RETURNED/LOST и РАЗНЫЕ игроки -> обновление статистики для КАЖДОГО игрока (общая + по лиге)
        if (betInDB.getBetStatus() != Bet.BetStatus.OPENED && !samePlayers) {
            updatePlayerStatsOnCompletedBet(statsOfPreviousPlayer, previousStateOfBet, false);
            updatePlayerStatsOnCompletedBet(statsOfActualPlayer, betInDB, true);
            updateTeamsStatsOnCompletedBet(statsByTeamsOfPreviousPlayer, previousStateOfBet, false);
            updateTeamsStatsOnCompletedBet(statsByTeamsOfActualPlayer, betInDB, true);
            updateTeamsStatsOnCompletedBet(leagueStatsByTeams, previousStateOfBet, false);
            updateTeamsStatsOnCompletedBet(leagueStatsByTeams, betInDB, true);
        }

        playerStatsRepository.saveAll(List.of(statsOfPreviousPlayer, statsOfActualPlayer));
        if (statsByTeamsOfPreviousPlayer != null) {
            playerStatsByTeamsRepository.save(statsByTeamsOfPreviousPlayer);
        }
        if (statsByTeamsOfActualPlayer != null) {
            playerStatsByTeamsRepository.save(statsByTeamsOfActualPlayer);
        }
        if (leagueStatsByTeams != null) {
            playerStatsByTeamsRepository.save(leagueStatsByTeams);
        }
    }

    private void updatePlayerStatsOnCompletedBet(PlayerStats playerStats, Bet bet, boolean isStatsToAppend) {
        int multiplier = isStatsToAppend ? 1 : -1;

        playerStats.setTotalBets(playerStats.getTotalBets() + multiplier);
        playerStats.setBetCount(playerStats.getBetCount() + multiplier);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() + multiplier * bet.getBetOdds());
        playerStats.setActualBalance(playerStats.getActualBalance() + multiplier * bet.getBalanceChange());

        if (bet.getBetStatus() == Bet.BetStatus.WON) {
            playerStats.setWonBetCount(playerStats.getWonBetCount() + multiplier);
            playerStats.setSumOfWonOdds(playerStats.getSumOfWonOdds() + multiplier * bet.getBetOdds());
        }
        if (bet.getBetStatus() == Bet.BetStatus.RETURNED) {
            playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() + multiplier);
        }
        if (bet.getBetStatus() == Bet.BetStatus.LOST) {
            playerStats.setLostBetCount(playerStats.getLostBetCount() + multiplier);
        }

        recalculatePlayerStats(playerStats);
    }

    private void updateTeamsStatsOnCompletedBet(PlayerStatsByTeams playerStatsByTeams, Bet bet, boolean isStatsToAppend) {
        if (playerStatsByTeams == null) return;

        int multiplier = isStatsToAppend ? 1 : -1;
        String homeTeamId = bet.getHomeTeam().getId();
        String awayTeamId = bet.getAwayTeam().getId();
        boolean homeTeamStatsExist = false;
        boolean awayTeamStatsExist = false;

        for (TeamStats teamStats : playerStatsByTeams.getTeamStats()) {
            String teamId = teamStats.getTeam().getId();
            if (teamId.equals(homeTeamId)) {
                homeTeamStatsExist = true;
            }
            if (teamId.equals(awayTeamId)) {
                awayTeamStatsExist = true;
            }
            if (teamId.equals(homeTeamId) || teamId.equals(awayTeamId)) {
                teamStats.setBetCount(teamStats.getBetCount() + multiplier);
                teamStats.setSumOfOdds(teamStats.getSumOfOdds() + multiplier * bet.getBetOdds());
                teamStats.setActualBalance(teamStats.getActualBalance() + multiplier * bet.getBalanceChange());

                if (bet.getBetStatus() == Bet.BetStatus.WON) {
                    teamStats.setWonBetCount(teamStats.getWonBetCount() + multiplier);
                    teamStats.setSumOfWonOdds(teamStats.getSumOfWonOdds() + multiplier * bet.getBetOdds());
                }
                if (bet.getBetStatus() == Bet.BetStatus.RETURNED) {
                    teamStats.setReturnedBetCount(teamStats.getReturnedBetCount() + multiplier);
                }
                if (bet.getBetStatus() == Bet.BetStatus.LOST) {
                    teamStats.setLostBetCount(teamStats.getLostBetCount() + multiplier);
                }

                recalculateStatsByTeams(teamStats);

            }
        }

        playerStatsByTeams.getTeamStats().removeIf(teamStats -> teamStats.getBetCount() == 0);

        if (!homeTeamStatsExist && isStatsToAppend) {
            TeamStats newTeamStats = createNewTeamStats(bet.getHomeTeam(), bet);
            playerStatsByTeams.getTeamStats().add(newTeamStats);
        }
        if (!awayTeamStatsExist && isStatsToAppend) {
            TeamStats newTeamStats = createNewTeamStats(bet.getAwayTeam(), bet);
            playerStatsByTeams.getTeamStats().add(newTeamStats);
        }
    }

    private TeamStats createNewTeamStats(Team team, Bet bet) {
        TeamStats teamStats = TeamStats.builder()
                .team(team)
                .betCount(1)
                .wonBetCount(0)
                .returnedBetCount(0)
                .lostBetCount(0)
                .sumOfOdds(bet.getBetOdds())
                .sumOfWonOdds(0.0)
                .actualBalance(bet.getBalanceChange())
                .build();
        if (bet.getBetStatus() == Bet.BetStatus.WON) {
            teamStats.setWonBetCount(teamStats.getWonBetCount() + 1);
            teamStats.setSumOfWonOdds(teamStats.getSumOfWonOdds() + bet.getBetOdds());
        }
        if (bet.getBetStatus() == Bet.BetStatus.RETURNED) {
            teamStats.setReturnedBetCount(teamStats.getReturnedBetCount() + 1);
        }
        if (bet.getBetStatus() == Bet.BetStatus.LOST) {
            teamStats.setLostBetCount(teamStats.getLostBetCount() + 1);
        }

        recalculateStatsByTeams(teamStats);
        return teamStats;
    }


    // ------------------------------------------------------------------------------------------------------ //
    // группа методов для УДАЛЕНИЯ ставки и пересчёта всей связанной статистики

    @Override
    @Transactional
    public BetDto deleteBet(String moderatorId, String betId, DeletedBetDto deletedBetMetaData) {
        Bet bet = getBetOrThrow(betsRepository, betId);
        User moderator = getUserOrThrow(usersRepository, moderatorId);
        String homeTeamId = "";
        String awayTeamId = "";
        PlayerStatsByTeams playerStatsByTeams = null;
        PlayerStatsByTeams leagueStatsByTeams = null;
        if (bet.getBetStatus() != Bet.BetStatus.EMPTY) {
            homeTeamId = bet.getHomeTeam().getId();
            awayTeamId = bet.getAwayTeam().getId();
            playerStatsByTeams = getPlayerStatsByTeamsOrNull(playerStatsByTeamsRepository, bet.getSeason().getId(), bet.getLeague().getId(), bet.getUser(), false);
            leagueStatsByTeams = getLeagueStatsByTeamsOrNull(playerStatsByTeamsRepository, bet.getSeason().getId(), bet.getLeague().getId(), true);
        }

        PlayerStats playerStats = getPlayerStatsOrThrow(playerStatsRepository, deletedBetMetaData.getSeasonId(), deletedBetMetaData.getLeagueId(), bet.getUser());

        processStats(bet, homeTeamId, awayTeamId, playerStats, playerStatsByTeams, leagueStatsByTeams);

        recalculatePlayerStats(playerStats);
        playerStatsRepository.save(playerStats);
        if (bet.getBetStatus() != Bet.BetStatus.EMPTY && playerStatsByTeams != null && leagueStatsByTeams != null) {
            playerStatsByTeamsRepository.saveAll(List.of(playerStatsByTeams, leagueStatsByTeams));
        }

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

            if (!bet.getBetStatus().equals(Bet.BetStatus.EMPTY)) {
                if (playerStatsByTeams != null) {
                    processTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
                }
                if (leagueStatsByTeams != null) {
                    processTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
                }
            }
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

        if (playerStatsByTeams != null) {
            processWonTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
        if (leagueStatsByTeams != null) {
            processWonTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
    }

    private void processReturnedStats(PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams, String homeTeamId, String awayTeamId, Bet bet) {
        playerStats.setReturnedBetCount(playerStats.getReturnedBetCount() - 1);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());

        if (playerStatsByTeams != null) {
            processReturnedTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
        if (leagueStatsByTeams != null) {
            processReturnedTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
    }

    private void processLostStats(PlayerStats playerStats, PlayerStatsByTeams playerStatsByTeams, PlayerStatsByTeams leagueStatsByTeams, String homeTeamId, String awayTeamId, Bet bet) {
        playerStats.setLostBetCount(playerStats.getLostBetCount() - 1);
        playerStats.setSumOfOdds(playerStats.getSumOfOdds() - bet.getBetOdds());

        if (playerStatsByTeams != null) {
            processLostTeamStats(playerStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
        if (leagueStatsByTeams != null) {
            processLostTeamStats(leagueStatsByTeams.getTeamStats(), homeTeamId, awayTeamId, bet);
        }
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



