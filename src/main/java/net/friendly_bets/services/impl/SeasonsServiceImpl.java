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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.friendly_bets.utils.GetEntityOrThrow.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonsServiceImpl implements SeasonsService {

    SeasonsRepository seasonsRepository;
    UsersRepository usersRepository;
    LeaguesRepository leaguesRepository;
    TeamsRepository teamsRepository;
    MongoTemplate mongoTemplate;
    private final BetsRepository betsRepository;

    @Override
    @Transactional
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

        Season season = getSeasonOrThrow(seasonsRepository, seasonId);

        if (season.getStatus().toString().equals(status)) {
            throw new ConflictException("Сезон уже имеет этот статус");
        }

//        TODO: стоит ли запретить менять статус окончненных турниров??
//        if (season.getStatus().equals(Season.Status.FINISHED)) {
//            throw new BadRequestException("Сезон завершен и его статус больше нельзя изменить");
//        }

        if (status.equals("ACTIVE")) {
            seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE)
                    .ifPresent(s -> {
                        s.setStatus(Season.Status.PAUSED);
                        seasonsRepository.save(s);
                    });
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
    public List<String> getLeagueCodeList() {
        return Arrays.stream(League.LeagueCode.values())
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
        User user = getUserOrThrow(usersRepository, userId);
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
        League.LeagueCode leagueCode;

        try {
            leagueCode = League.LeagueCode.valueOf(newLeague.getLeagueCode());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Недопустимый статус: " + newLeague.getLeagueCode());
        }

        if (season.getLeagues().stream().anyMatch(l -> l.getLeagueCode().equals(leagueCode))) {
            throw new ConflictException("Лига с таким названием уже существует в этом турнире");
        }

        League league = League.builder()
                .createdAt(LocalDateTime.now())
                .leagueCode(League.LeagueCode.valueOf(newLeague.getLeagueCode()))
                .name(newLeague.getLeagueCode() + " " + season.getTitle())
                .currentMatchDay("1")
                .teams(new ArrayList<>())
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
    public String dbUpdate() {
        List<Team> allTeams = teamsRepository.findAll();

        for (Team team : allTeams) {
            String title = team.getTitle();
            String replaced = title.replace(" ", "");
            if (!title.equals(replaced)) {
                team.setTitle(replaced);
                teamsRepository.save(team);
            }
        }

        List<Bet> allBets = betsRepository.findAll();

        for (Bet bet : allBets) {
            String matchDay = bet.getMatchDay();
            if (matchDay != null && matchDay.startsWith("1/")) {
                int index = matchDay.indexOf(" ");
                if (index != -1) {
                    String res = matchDay.substring(0, index);
                    bet.setMatchDay(res);
                    betsRepository.save(bet);
                }
            }
        }


//            // Создаем запрос для обновления конкретного документа
//            Query individualQuery = new Query().addCriteria(Criteria.where("_id").is(team.getId()));
//
//            // Создаем обновление для добавления поля "title" и удаления полей "fullTitleEn" и "fullTitleRu"
//            Update update = new Update()
//                    .set("title", team.getFullTitleEn())
//                    .unset("fullTitleEn")
//                    .unset("fullTitleRu");
//
//            // Выполняем обновление для конкретного документа
//            mongoTemplate.updateFirst(individualQuery, update, Team.class);


        return "DB update complete";
    }

    // ------------------------------------------------------------------------------------------------------ //

}
