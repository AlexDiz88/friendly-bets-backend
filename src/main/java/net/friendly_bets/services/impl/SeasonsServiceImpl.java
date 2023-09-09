package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.SeasonsService;
import net.friendly_bets.utils.BetValuesUtils;
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
public class SeasonsServiceImpl implements SeasonsService {

    private final SeasonsRepository seasonsRepository;
    private final UsersRepository usersRepository;
    private final LeaguesRepository leaguesRepository;
    private final TeamsRepository teamsRepository;
    private final BetsRepository betsRepository;

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
            return new SeasonDto();
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto getScheduledSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
        if (seasonByStatus.isEmpty()) {
            return new SeasonDto();
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
                .leagues(LeagueDto.from(season.getLeagues()))
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
    public SeasonDto addTeamToLeagueInSeason(String seasonId, String leagueId, String teamId) {
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
        seasonsRepository.save(season);

        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto addBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewBetDto newBet) {
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
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto addEmptyBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewEmptyBetDto newEmptyBet) {
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
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public SeasonDto addBetResult(String moderatorId, String seasonId, String betId, NewBetResult newBetResult) {
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
        Season season =  getSeasonOrThrow(seasonsRepository, seasonId);

        Bet.BetStatus betStatus = Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        setBalanceChange(bet, betStatus);

        bet.setBetResultAddedAt(LocalDateTime.now());
        bet.setBetResultAddedBy(moderator);
        bet.setBetStatus(betStatus);
        bet.setGameResult(newBetResult.getGameResult());
        betsRepository.save(bet);

        return SeasonDto.from(season);
    }
}
