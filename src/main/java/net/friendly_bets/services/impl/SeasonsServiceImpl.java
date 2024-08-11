package net.friendly_bets.services.impl;

import com.mongodb.client.result.UpdateResult;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    MongoTemplate mongoTemplate;

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
            throw new BadRequestException("seasonWithThisTitleAlreadyExist");
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
            throw new BadRequestException("seasonStatusIsNull");
        }
        status = status.substring(1, status.length() - 1);
        try {
            Season.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalidStatus");
        }

        Season season = getSeasonOrThrow(seasonsRepository, seasonId);

        if (season.getStatus().toString().equals(status)) {
            throw new ConflictException("seasonAlreadyHasSameStatus");
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
            throw new BadRequestException("noActiveSeasonWasFounded");
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public ActiveSeasonIdDto getActiveSeasonId() {
        Optional<Season> activeSeason = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (activeSeason.isEmpty()) {
            throw new BadRequestException("noActiveSeasonWasFounded");
        }
        return new ActiveSeasonIdDto(activeSeason.get().getId());
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto getScheduledSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("noScheduledSeasonWasFounded");
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
            throw new BadRequestException("fillUsernameInProfile");
        }
        if (user.getRole().equals(User.Role.ADMIN)) {
            throw new ConflictException("administratorNotAllowedRegisterInSeason");
        }
        if (season.getPlayers().stream().anyMatch(player -> player.getId().equals(userId))) {
            throw new ConflictException("youAlreadyRegisteredInSeason");
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
            throw new BadRequestException("invalidStatus");
        }

        if (season.getLeagues().stream().anyMatch(l -> l.getLeagueCode().equals(leagueCode))) {
            throw new ConflictException("leagueAlreadyExistInThisSeason");
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
            throw new BadRequestException("teamIdIsNull");
        }
        if (leagueId == null || leagueId.isBlank()) {
            throw new BadRequestException("leagueIdIsNull");
        }
        if (seasonId == null || seasonId.isBlank()) {
            throw new BadRequestException("seasonIdIsNull");
        }
        Season season = getSeasonOrThrow(seasonsRepository, seasonId);
        Team team = getTeamOrThrow(teamsRepository, teamId);

        Optional<League> optionalLeague = season.getLeagues().stream().filter(l -> l.getId().equals(leagueId)).findFirst();
        if (optionalLeague.isEmpty()) {
            throw new NotFoundException("League", leagueId);
        }

        League leagueInSeason = optionalLeague.get();
        if (leagueInSeason.getTeams().stream().anyMatch(t -> t.getId().equals(teamId))) {
            throw new ConflictException("teamAlreadyExistInLeagueInThisSeason");
        }

        leagueInSeason.getTeams().add(team);
        leaguesRepository.save(leagueInSeason);

        return TeamDto.from(team);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    @Transactional
    public Map<String, String> dbUpdate() {
        // Инициализация счетчиков
        int totalBets = 0;
        int euro24playoff = 0;
        int nullOrBlankMatchDayCount = 0;
        int startsWithOneSlashCount = 0;
        int validMatchDayCount = 0;
        int updatedMatchDayCount = 0;
        int removedFieldsCount = 0;
        int nonStandardMatchDayCount = 0;

        // Найти все ставки с указанным идентификатором сезона
        List<Bet> bets = betsRepository.findAllBySeason_Id("666af3277f505e49026dbf41");

        for (Bet bet : bets) {
            totalBets++;  // Увеличение общего счетчика ставок
            String matchDay = bet.getMatchDay();
            String playoffRound = bet.getPlayoffRound();

            // Если matchDay равен null или пуст, вывести ID и пропустить
            if (matchDay == null || matchDay.isBlank()) {
                nullOrBlankMatchDayCount++;
                System.out.println("Bet with null or blank match_day: " + bet.getId());
                continue;
            }

            boolean shouldUpdate = false;
            String newMatchDay = matchDay;

            // Проверка, соответствует ли matchDay критериям для обновления
            if (matchDay.startsWith("1/") && !matchDay.contains("[") && !matchDay.contains(" ") && playoffRound != null && !playoffRound.isBlank()) {
                newMatchDay = matchDay + " [" + playoffRound + "]";
                startsWithOneSlashCount++;
                shouldUpdate = true;
            }

            if (matchDay.equals("1/8") || matchDay.equals("1/4") || matchDay.equals("1/2")) {
                euro24playoff++;
                shouldUpdate = true;
            }

            if (shouldUpdate) {
                // Создание объекта обновления
                Update update = new Update();

                // Если matchDay изменился, установить новое значение
                if (!matchDay.equals(newMatchDay)) {
                    update.set("match_day", newMatchDay);
                    updatedMatchDayCount++;
                }

                // Удалить поля is_playoff и playoff_round, если необходимо
                update.unset("is_playoff")
                        .unset("playoff_round");
                removedFieldsCount++;

                // Выполнение обновления
                UpdateResult result = mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(bet.getId())), update, Bet.class);

                if (result.getMatchedCount() == 0) {
                    // Логирование, если документ не был найден для обновления
                    System.out.println("No document matched for ID: " + bet.getId());
                }
            } else {
                nonStandardMatchDayCount++;
                System.out.println("Нестандартный тур у ставки:" + bet.getId());
            }
        }

        // Подготовка ответа с количеством обработанных ставок
        System.out.println("Общее число ставок : " + totalBets);
        System.out.println("Евро24 плейофф : " + euro24playoff);
        System.out.println("matchDay равные null либо пустой строке : " + nullOrBlankMatchDayCount);
        System.out.println("Валидных ставок плейоффа 1/Х которым будут добавлять скобки [Y] : " + startsWithOneSlashCount);
        System.out.println("Ставок обычных туров либо финалов: " + validMatchDayCount);
        System.out.println("Число ставок которым установили новое значение в поле matchDay : " + updatedMatchDayCount);
        System.out.println("Число ставок которым удалили поля isPlayoff и playoffRound: " + removedFieldsCount);
        System.out.println("Ставок которые не попали ни в одно условие : " + nonStandardMatchDayCount);

        Map<String, String> response = new HashMap<>();
        response.put("message", "DB update complete");
        response.put("totalBets", String.valueOf(totalBets));
        response.put("nullOrBlankMatchDayCount", String.valueOf(nullOrBlankMatchDayCount));
        response.put("startsWithOneSlashCount", String.valueOf(startsWithOneSlashCount));
        response.put("validMatchDayCount", String.valueOf(validMatchDayCount));
        response.put("updatedMatchDayCount", String.valueOf(updatedMatchDayCount));
        response.put("removedFieldsCount", String.valueOf(removedFieldsCount));
        response.put("nonStandardMatchDayCount", String.valueOf(nonStandardMatchDayCount));

        return response;
    }


    // ------------------------------------------------------------------------------------------------------ //

}
