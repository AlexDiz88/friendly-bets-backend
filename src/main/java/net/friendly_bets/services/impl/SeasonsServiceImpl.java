package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.*;
import net.friendly_bets.services.SeasonsService;
import net.friendly_bets.utils.GameResultValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    public SeasonDto addSeason(NewSeasonDto newSeason) {
        if (newSeason == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newSeason.getTitle() == null || newSeason.getTitle().trim().length() < 1) {
            throw new BadDataException("Название сезона не может быть пустым");
        }
        if (newSeason.getBetCountPerMatchDay() == null || newSeason.getBetCountPerMatchDay() < 1) {
            throw new BadDataException("Количество ставок на тур не может быть меньше 1");
        }
        if (seasonsRepository.existsByTitle(newSeason.getTitle())) {
            throw new BadDataException("Сезон с таким названием уже существует");
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
    public SeasonDto changeSeasonStatus(String id, String status) {
        if (status == null) {
            throw new BadDataException("Статус сезона is null");
        }
        status = status.substring(1, status.length() - 1);
        try {
            Season.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadDataException("Недопустимый статус: " + status);
        }
        Season season = seasonsRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Сезон с таким ID не найден")
        );
        if (season.getStatus().toString().equals(status)) {
            throw new ConflictException("Сезон уже имеет этот статус");
        }
        if (season.getStatus().equals(Season.Status.FINISHED)) {
            throw new BadDataException("Сезон завершен и его статус больше нельзя изменить");
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
    public SeasonDto registrationInSeason(String userId, String seasonId) {
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон с таким ID не найден")
        );
        User user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь не найден")
        );
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BadDataException("Сначала заполните поле 'Имя' в личном кабинете");
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
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон с таким ID не найден")
        );
        return LeaguesPage.builder()
                .leagues(LeagueDto.from(season.getLeagues()))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague) {
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон с таким ID не найден")
        );
        if (newLeague == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newLeague.getDisplayNameRu().trim().equals("") || newLeague.getDisplayNameEn().trim().equals("")) {
            throw new BadDataException("Название лиги (RU/EN) не может быть пустым");
        }
        if (newLeague.getShortNameRu().trim().equals("") || newLeague.getShortNameEn().trim().equals("")) {
            throw new BadDataException("Сокращенное имя лиги (RU/EN) не может быть пустым");
        }
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
    public SeasonDto addTeamToLeagueInSeason(String seasonId, String leagueId, String teamId) {
        if (teamId == null || teamId.isBlank()) {
            throw new BadDataException("Команда не выбрана");
        }
        if (leagueId == null || leagueId.isBlank()) {
            throw new BadDataException("Лига не выбрана");
        }
        if (seasonId == null || seasonId.isBlank()) {
            throw new BadDataException("Сезон не выбран");
        }
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new NotFoundException("Сезон с таким ID не найден")
        );
        Team team = teamsRepository.findById(teamId).orElseThrow(
                () -> new NotFoundException("Команда с таким ID не найдена")
        );

        Optional<League> optionalLeague = season.getLeagues().stream().filter(l -> l.getId().equals(leagueId)).findFirst();
        if (optionalLeague.isEmpty()) {
            throw new NotFoundException("Лига с таким ID в этом сезоне не найдена");
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
    public SeasonDto addBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewBetDto newBet) {
        if (newBet == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newBet.getMatchDay() == null || newBet.getMatchDay().isBlank()) {
            throw new BadDataException("Игровой тур не указан");
        }
        if (newBet.getHomeTeamId().equals(newBet.getAwayTeamId())) {
            throw new BadDataException("Команда хозяев не может совпадать с командой гостей");
        }
        if (newBet.getBetTitle() == null || newBet.getBetTitle().isBlank()) {
            throw new BadDataException("Ставка не указана");
        }
        if (newBet.getBetOdds() == null) {
            throw new BadDataException("Коэффициент ставки не указан, либо указан неверно");
        }
        if (newBet.getBetOdds().isNaN()) {
            throw new BadDataException("Коэффициент ставки не является числом");
        }
        if (newBet.getBetOdds() <= 1) {
            throw new BadDataException("Коэффициент ставки не может быть меньше чем 1,01");
        }
        if (newBet.getBetSize() == null) {
            throw new BadDataException("Размер ставки не указан");
        }
        if (newBet.getBetSize() < 1) {
            throw new BadDataException("Размер ставки не может быть меньше 1");
        }

        User moderator = usersRepository.findById(moderatorId).orElseThrow(
                () -> new NotFoundException("Модератор с таким ID не найден")
        );
        User user = usersRepository.findById(newBet.getUserId()).orElseThrow(
                () -> new NotFoundException("Участник с таким ID не найден")
        );
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID сезона")
        );
        League league = leaguesRepository.findById(leagueId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID лиги")
        );
        Team homeTeam = teamsRepository.findById(newBet.getHomeTeamId()).orElseThrow(
                () -> new NotFoundException("Команда хозяев с ID <" + newBet.getHomeTeamId() + "> не найдена")
        );
        Team awayTeam = teamsRepository.findById(newBet.getAwayTeamId()).orElseThrow(
                () -> new NotFoundException("Команда гостей с ID <" + newBet.getAwayTeamId() + "> не найдена")
        );

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
        if (season.getPlayers().size() != 0 && season.getBetCountPerMatchDay() != 0) {
            System.out.println("Пересчет текущего тура -----------------");
            System.out.println("Кол-во ставок в лиге: " + league.getBets().size());
            int totalBets = (int) league.getBets().stream().filter(b -> !b.getBetStatus().equals(Bet.BetStatus.DELETED)).count();
            int currentMatchDay = totalBets / (season.getPlayers().size() * season.getBetCountPerMatchDay()) + 1;
            System.out.println("currentMatchDay:" + currentMatchDay);
            league.setCurrentMatchDay(String.valueOf(currentMatchDay));
        }

        leaguesRepository.save(league);
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto addEmptyBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewEmptyBetDto newEmptyBet) {
        if (newEmptyBet == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newEmptyBet.getMatchDay() == null || newEmptyBet.getMatchDay().isBlank()) {
            throw new BadDataException("Игровой тур не указан");
        }
        if (newEmptyBet.getBetSize() == null) {
            throw new BadDataException("Размер ставки не указан");
        }
        if (newEmptyBet.getBetSize() < 1) {
            throw new BadDataException("Размер ставки не может быть меньше 1");
        }

        User moderator = usersRepository.findById(moderatorId).orElseThrow(
                () -> new NotFoundException("Модератор с таким ID не найден")
        );
        User user = usersRepository.findById(newEmptyBet.getUserId()).orElseThrow(
                () -> new NotFoundException("Участник с таким ID не найден")
        );
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID сезона")
        );
        League league = leaguesRepository.findById(leagueId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID лиги")
        );

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
        if (season.getPlayers().size() != 0 && season.getBetCountPerMatchDay() != 0) {
            System.out.println("Пересчет текущего тура -----------------");
            System.out.println("Кол-во ставок в лиге: " + league.getBets().size());
            int totalBets = (int) league.getBets().stream().filter(b -> !b.getBetStatus().equals(Bet.BetStatus.DELETED)).count();
            int currentMatchDay = totalBets / (season.getPlayers().size() * season.getBetCountPerMatchDay()) + 1;
            System.out.println("currentMatchDay:" + currentMatchDay);
            league.setCurrentMatchDay(String.valueOf(currentMatchDay));
        }

        leaguesRepository.save(league);
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto addBetResult(String moderatorId, String seasonId, String betId, NewBetResult newBetResult) {
        if (newBetResult == null) {
            throw new BadDataException("Объект не должен быть пустым");
        }
        if (newBetResult.getBetStatus() == null || newBetResult.getBetStatus().isBlank()) {
            throw new BadDataException("Статус ставки не может быть пустым");
        }
        if (newBetResult.getGameResult() == null || newBetResult.getGameResult().isBlank()) {
            throw new BadDataException("Счёт матча не может быть пустым");
        }

        try {
            Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый статус: " + newBetResult.getBetStatus());
        }
        if (!GameResultValidator.isValidGameResult(newBetResult.getGameResult())) {
            throw new BadDataException("Некорректный счёт матча: " + newBetResult.getGameResult());
        }

        Bet bet = betsRepository.findById(betId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID ставки")
        );
        if (!bet.getBetStatus().equals(Bet.BetStatus.OPENED)) {
            throw new ConflictException("Эта ставка уже обработана другим модератором");
        }

        User moderator = usersRepository.findById(moderatorId).orElseThrow(
                () -> new NotFoundException("Модератор с таким ID не найден")
        );
        Season season = seasonsRepository.findById(seasonId).orElseThrow(
                () -> new IllegalArgumentException("Ошибка ID сезона")
        );


        Bet.BetStatus status = Bet.BetStatus.valueOf(newBetResult.getBetStatus());
        if (status.equals(Bet.BetStatus.WON)) {
            bet.setBalanceChange(bet.getBetOdds() * bet.getBetSize() - bet.getBetSize());
        }
        if (status.equals(Bet.BetStatus.RETURNED)) {
            bet.setBalanceChange(0.0);
        }
        if (status.equals(Bet.BetStatus.LOST)) {
            bet.setBalanceChange(-Double.valueOf(bet.getBetSize()));
        }

        bet.setBetResultAddedAt(LocalDateTime.now());
        bet.setBetResultAddedBy(moderator);
        bet.setBetStatus(status);
        bet.setGameResult(newBetResult.getGameResult());
        betsRepository.save(bet);

        return SeasonDto.from(season);
    }
}
