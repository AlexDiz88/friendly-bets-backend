package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.SeasonsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.friendly_bets.utils.BetValuesUtils.*;
import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonsServiceImpl implements SeasonsService {

    SeasonsRepository seasonsRepository;
    UsersRepository usersRepository;
    LeaguesRepository leaguesRepository;
    TeamsRepository teamsRepository;
    BetsRepository betsRepository;
    PlayerStatsRepository playerStatsRepository;

    @Override
    public SeasonsPage getAll() {
        List<Season> allSeasons = seasonsRepository.findAll();
        return SeasonsPage.builder()
                .seasons(SeasonDto.from(allSeasons))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto addSeason(NewSeasonDto newSeason) {
        if (seasonsRepository.existsByTitle(newSeason.getTitle())) {
            throw new BadRequestException("Сезон с таким названием уже существует");
        }

        Season season = Season.builder()
                .createdAt(LocalDateTime.now())
                .title(newSeason.getTitle())
                .betCountPerMatchDay(newSeason.getBetCountPerMatchDay())
                .status(Season.Status.CREATED)
                .players(new ArrayList<>())
                .leagues(new ArrayList<>())
                .build();

        seasonsRepository.save(season);
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto changeSeasonStatus(String seasonId, String status) {
        if (status == null) {
            throw new BadRequestException("Статус сезона is null");
        }
        status = status.substring(1, status.length() - 1);
        try {
            Season.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Недопустимый статус: " + status);
        }
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Cезон", seasonId)
        );
        if (season.getStatus().toString().equals(status)) {
            throw new ConflictException("Сезон уже имеет этот статус");
        }
        if (season.getStatus().equals(Season.Status.FINISHED)) {
            throw new BadRequestException("Сезон завершен и его статус больше нельзя изменить");
        }

        if (status.equals("ACTIVE")) {
            Optional<Season> currentActiveSeason = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
            currentActiveSeason.ifPresent(value -> value.setStatus(Season.Status.PAUSED));
        }

        season.setStatus(Season.Status.valueOf(status));
        seasonsRepository.save(season);

        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public List<String> getSeasonStatusList() {
        return Arrays.stream(Season.Status.values())
                .map(Enum::toString)
                .toList();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto getActiveSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("Сезон со статусом " + Season.Status.ACTIVE.name() + " не найден");
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public ActiveSeasonIdDto getActiveSeasonId() {
        Optional<Season> activeSeason = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (activeSeason.isEmpty()) {
            throw new BadRequestException("Сезон со статусом " + Season.Status.ACTIVE.name() + " не найден");
        }
        return new ActiveSeasonIdDto(activeSeason.get().getId());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto getScheduledSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("Сезон со статусом " + Season.Status.SCHEDULED.name() + " не найден");
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto registrationInSeason(String userId, String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        User user = getUserOrThrow(usersRepository, seasonId);
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BadRequestException("Сначала заполните поле 'Имя' в личном кабинете");
        }
        if (user.getRole().equals(User.Role.ADMIN)) {
            throw new ConflictException("Администратор не имеет права регистрироваться на турнирах");
        }
        if (season.getPlayers().stream().anyMatch(player -> player.getId().equals(userId))) {
            throw new ConflictException("Вы уже зарегистрированы на этом турнире");
        }

        season.getPlayers().add(user);
        seasonsRepository.save(season);
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public LeaguesPage getLeaguesBySeason(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        return LeaguesPage.builder()
                .leagues(LeagueDto.from(seasonId, season.getLeagues()))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);

        if (season.getLeagues().stream().anyMatch(l -> l.getDisplayNameRu().equals(newLeague.getDisplayNameRu()))) {
            throw new ConflictException("Лига с таким названием уже существует в этом турнире");
        }

        League league = League.builder()
                .createdAt(LocalDateTime.now())
                .name(newLeague.getDisplayNameRu() + "-" + season.getTitle())
                .displayNameRu(newLeague.getDisplayNameRu())
                .displayNameEn(newLeague.getDisplayNameEn())
                .shortNameRu(newLeague.getShortNameRu())
                .shortNameEn(newLeague.getShortNameEn())
                .currentMatchDay("1")
                .teams(new ArrayList<>())
                .bets(new ArrayList<>())
                .build();

        leaguesRepository.save(league);
        season.getLeagues().add(league);
        seasonsRepository.save(season);

        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public TeamDto addTeamToLeagueInSeason(String seasonId, String leagueId, String teamId) {
        if (teamId == null || teamId.isBlank()) {
            throw new BadRequestException("Команда не выбрана");
        }
        if (leagueId == null || leagueId.isBlank()) {
            throw new BadRequestException("Лига не выбрана");
        }
        if (seasonId == null || seasonId.isBlank()) {
            throw new BadRequestException("Сезон не выбран");
        }
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        Team team = getTeamOrThrow(teamsRepository, teamId);

        Optional<League> optionalLeague = season.getLeagues().stream().filter(l -> l.getId().equals(leagueId)).findFirst();
        if (optionalLeague.isEmpty()) {
            throw new NotFoundException("Лига", leagueId);
        }

        League leagueInSeason = optionalLeague.get();
        if (leagueInSeason.getTeams().stream().anyMatch(t -> t.getId().equals(teamId))) {
            throw new ConflictException("Эта команда уже добавлена в данную лигу в этом сезоне");
        }

        leagueInSeason.getTeams().add(team);
        leaguesRepository.save(leagueInSeason);

        return TeamDto.from(team);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto addBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewBetDto newBet) {
        checkTeams(newBet.getHomeTeamId(), newBet.getAwayTeamId());
        checkBetOdds(newBet.getBetOdds());

        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        League league = getLeagueOrThrow(leaguesRepository, leagueId);
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
        league.getBets().add(bet);
        setCurrentMatchDay(season, league);
        leaguesRepository.save(league);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(seasonId, leagueId, user));

        playerStats.setTotalBets(playerStats.getTotalBets() + 1);
        playerStatsRepository.save(playerStats);

        return BetDto.from(seasonId, leagueId, bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto addEmptyBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewEmptyBetDto newEmptyBet) {
        User moderator = getUserOrThrow(usersRepository, moderatorId);
        User user = getUserOrThrow(usersRepository, newEmptyBet.getUserId());
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        League league = getLeagueOrThrow(leaguesRepository, leagueId);

        Bet bet = Bet.builder()
                .createdAt(LocalDateTime.now())
                .createdBy(moderator)
                .user(user)
                .matchDay(newEmptyBet.getMatchDay())
                .betSize(newEmptyBet.getBetSize())
                .betStatus(Bet.BetStatus.EMPTY)
                .betResultAddedAt(LocalDateTime.now())
                .betResultAddedBy(moderator)
                .balanceChange(-Double.valueOf(newEmptyBet.getBetSize()))
                .build();

        betsRepository.save(bet);
        league.getBets().add(bet);
        setCurrentMatchDay(season, league);
        leaguesRepository.save(league);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, leagueId, user);
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(seasonId, leagueId, user));

        playerStats.setTotalBets(playerStats.getTotalBets() + 1);
        playerStats.setBetCount(playerStats.getBetCount() + 1);
        playerStats.setEmptyBetCount(playerStats.getEmptyBetCount() + 1);
        playerStats.setActualBalance(playerStats.getActualBalance() - Double.valueOf(newEmptyBet.getBetSize()));
        playerStatsRepository.save(playerStats);

        return BetDto.from(seasonId, leagueId, bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public BetDto addBetResult(String moderatorId, String seasonId, String betId, NewBetResult newBetResult) {
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
        League league = leaguesRepository.findByBets_Id(bet.getId());
        System.out.println("leagueId:" + league.getId());

        Bet.BetStatus betStatus = Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        setBalanceChange(bet, betStatus, bet.getBetSize(), bet.getBetOdds());

        bet.setBetResultAddedAt(LocalDateTime.now());
        bet.setBetResultAddedBy(moderator);
        bet.setBetStatus(betStatus);
        bet.setGameResult(newBetResult.getGameResult());
        betsRepository.save(bet);

        Optional<PlayerStats> playerStatsOptional = playerStatsRepository.findBySeasonIdAndLeagueIdAndUser(seasonId, league.getId(), bet.getUser());
        PlayerStats playerStats = playerStatsOptional.orElseGet(() -> getDefaultPlayerStats(seasonId, league.getId(), bet.getUser()));

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
        playerStats.calculateWinRate();
        playerStats.calculateAverageOdds();
        playerStats.calculateAverageWonBetOdds();
        playerStatsRepository.save(playerStats);

        return BetDto.from(seasonId, league.getId(), bet);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getAllOpenedBets(String seasonId) {
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        List<League> leagues = season.getLeagues();
        List<BetDto> openedBets = new ArrayList<>();
        for (League league : leagues) {
            List<Bet> bets = league.getBets();
            for (Bet bet : bets) {
                if (bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
                    openedBets.add(BetDto.from(seasonId, league.getId(), bet));
                }
            }
        }
        return BetsPage.builder()
                .bets(openedBets)
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public BetsPage getAllCompletedBets(String seasonId, PageRequest pageRequest) {
        return null;
    }
}
