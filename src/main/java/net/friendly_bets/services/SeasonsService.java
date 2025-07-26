package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SeasonsService {

    MongoTemplate mongoTemplate;
    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    GetEntityService getEntityService;

    CalendarsRepository calendarsRepository;
    BetsRepository betsRepository;

    @Transactional
    public SeasonsPage getAll() {
        List<Season> allSeasons = seasonsRepository.findAll();
        return SeasonsPage.builder()
                .seasons(SeasonDto.from(allSeasons))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


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

        Season season = getEntityService.getSeasonOrThrow(seasonId);

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


    public List<String> getSeasonStatusList() {
        return Arrays.stream(Season.Status.values())
                .map(Enum::toString)
                .toList();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public List<String> getLeagueCodeList() {
        return Arrays.stream(League.LeagueCode.values())
                .map(Enum::toString)
                .toList();
    }

    // ------------------------------------------------------------------------------------------------------ //


    public SeasonDto getActiveSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("noActiveSeasonWasFounded");
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public ActiveSeasonIdDto getActiveSeasonId() {
        Optional<Season> activeSeason = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (activeSeason.isEmpty()) {
            throw new BadRequestException("noActiveSeasonWasFounded");
        }
        return new ActiveSeasonIdDto(activeSeason.get().getId());
    }

    // ------------------------------------------------------------------------------------------------------ //


    public SeasonDto getScheduledSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("noScheduledSeasonWasFounded");
        }
        Season season = seasonByStatus.get();
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public SeasonDto registrationInSeason(String userId, String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        User user = getEntityService.getUserOrThrow(userId);
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


    public LeaguesPage getLeaguesBySeason(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        return LeaguesPage.builder()
                .leagues(LeagueDto.from(season.getLeagues()))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public SeasonDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
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
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        Team team = getEntityService.getTeamOrThrow(teamId);

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

    public Map<String, Object> dbUpdate() {
        boolean execute = false;

        //noinspection ConstantConditions
        return execute ? dbUpdateExecution() : null;
    }

    // TODO: ошибка с лимитом ставок на тур при редактировании ставки
    @Transactional
    public Map<String, Object> dbUpdateExecution() {
        List<Bet> allBets = betsRepository.findAll();

        Map<String, BetTitleCode> labelToCodeMap = Arrays.stream(BetTitleCode.values())
                .collect(Collectors.toMap(BetTitleCode::getLabel, Function.identity()));

        Map<String, Object> failedConversions = new LinkedHashMap<>();
        int successCount = 0;
        int fallbackCount = 0;
        int emptyCount = 0;
        int notCount = 0;
        int not2Count = 0;

        for (Bet bet : allBets) {
            String originalTitle = bet.getBetTitle();
            String betId = bet.getId();

            if (originalTitle == null || originalTitle.isBlank()) {
                // Пустая или удалённая ставка — помечаем как EMPTY
                BetTitle emptyTitle = BetTitle.builder()
                        .code(BetTitleCode.EMPTY_BET_TITLE.getCode())
                        .isNot(false)
                        .label(BetTitleCode.EMPTY_BET_TITLE.getLabel())
                        .build();

                Update update = new Update().set("bet_title", emptyTitle);
                Query query = new Query(Criteria.where("id").is(betId));
//                mongoTemplate.updateFirst(query, update, Bet.class); // [REAL RUN]

                emptyCount++;
                continue;
            }

            boolean isNot = false;
            String title = originalTitle.trim();
            title = title.replaceAll("(\\d+),(\\d+)", "$1.$2"); // меняем запятые на точки

            if (title.endsWith(" - нет")) {
                title = title.substring(0, title.length() - " - нет".length()).trim();
                isNot = true;
                notCount++;
            } else if (title.endsWith("- нет")) {
                title = title.substring(0, title.length() - "- нет".length()).trim();
                isNot = true;
                not2Count++;
            }

            BetTitleCode betTitleCode = labelToCodeMap.get(title);
            if (betTitleCode == null) {
                // Ошибка конвертации: очищаем основное поле, сохраняем исходную строку
                Update fallbackUpdate = new Update()
                        .set("bet_title", null)
                        .set("bet_title_temp", originalTitle);
                Query fallbackQuery = new Query(Criteria.where("id").is(betId));

//                mongoTemplate.updateFirst(fallbackQuery, fallbackUpdate, Bet.class); // [REAL RUN]

                log.warn("⚠️ Failed to convert betId={}, betTitle='{}', betStatus={}", betId, originalTitle, bet.getBetStatus());
                failedConversions.put(betId, originalTitle);
                fallbackCount++;
                continue;
            }

            BetTitle newBetTitle = BetTitle.builder()
                    .code(betTitleCode.getCode())
                    .isNot(isNot)
                    .label(betTitleCode.getLabel())
                    .build();

            Update update = new Update().set("bet_title", newBetTitle);
            Query query = new Query(Criteria.where("id").is(betId));

//            mongoTemplate.updateFirst(query, update, Bet.class); // [REAL RUN]

            successCount++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mode", "dry-run");
        response.put("totalBets", allBets.size());
        response.put("successCount", successCount);
        response.put(" - нет", notCount);
        response.put("- нет", not2Count);
        response.put("emptyCount", emptyCount);
        response.put("fallbackCount", fallbackCount);
        response.put("failedItems", failedConversions);

        return response;
    }

}
