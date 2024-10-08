package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonsService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;

    GetEntityService getEntityService;

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


    @Transactional
    public Map<String, String> dbUpdate() {
//        List<Bet> bets = betsRepository.findAll();
//
//        for (Bet bet : bets) {
//            String gameResultStr = bet.getGameResult();
//            if (gameResultStr != null) {
//                GameResult gameResult = parseGameResult(gameResultStr);
//
//
//                Update update = new Update().set("gameResult", gameResult);
//                Query query = new Query(Criteria.where("id").is(bet.getId()));
//                mongoTemplate.updateFirst(query, update, Bet.class);
//            }
//        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "DB update complete");
        return response;
    }

}
