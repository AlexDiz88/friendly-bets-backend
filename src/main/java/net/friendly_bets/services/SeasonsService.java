package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.*;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.CalendarsRepository;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import net.friendly_bets.utils.SeasonCalendarUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonsService {

    SeasonsRepository seasonsRepository;
    LeaguesRepository leaguesRepository;
    GetEntityService getEntityService;

    CalendarsRepository calendarsRepository;
    BetsRepository betsRepository;
    TournamentFormatsRepository tournamentFormatsRepository;
    TournamentFormatExpander tournamentFormatExpander;
    LeagueMatchdayService leagueMatchdayService;
    RunningSeasonLookup runningSeasonLookup;

    @Transactional
    public SeasonsPage getAll() {
        List<Season> allSeasons = seasonsRepository.findAll();
        return SeasonsPage.builder()
                .seasons(allSeasons.stream().map(this::toSeasonDto).toList())
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Transactional
    public SeasonDto addSeason(NewSeasonDto newSeason) {
        if (seasonsRepository.existsByTitle(newSeason.getTitle())) {
            throw new BadRequestException("seasonWithThisTitleAlreadyExist");
        }
        SeasonCalendarUtils.validateDateRange(newSeason.getStartDate(), newSeason.getEndDate());

        Season season = Season.builder()
                .createdAt(Instant.now())
                .title(newSeason.getTitle())
                .startDate(newSeason.getStartDate())
                .endDate(newSeason.getEndDate())
                .betCountPerMatchDay(newSeason.getBetCountPerMatchDay())
                .defaultBetSize(newSeason.getDefaultBetSize())
                .status(Season.Status.CREATED)
                .players(new ArrayList<>())
                .leagues(new ArrayList<>())
                .build();

        seasonsRepository.save(season);
        return toSeasonDto(season);
    }

    @Transactional(readOnly = true)
    public SeasonsWithoutDatesPage getSeasonsWithoutDates() {
        List<SeasonWithoutDatesDto> seasons = seasonsRepository.findByStartDateIsNull().stream()
                .map(SeasonWithoutDatesDto::from)
                .collect(Collectors.toList());
        return SeasonsWithoutDatesPage.builder().seasons(seasons).build();
    }

    @Transactional
    public SeasonDto assignSeasonDates(String seasonId, UpdateSeasonDatesDto dto) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        SeasonCalendarUtils.validateDateRange(dto.getStartDate(), dto.getEndDate());
        season.setStartDate(dto.getStartDate());
        season.setEndDate(dto.getEndDate());
        seasonsRepository.save(season);
        return toSeasonDto(season);
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

        if (Season.Status.ACTIVE.name().equals(status) || Season.Status.SCHEDULED.name().equals(status)) {
            runningSeasonLookup.pauseOtherRunningSeasons(seasonId);
        }

        season.setStatus(Season.Status.valueOf(status));
        seasonsRepository.save(season);

        return toSeasonDto(season);
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
        Season season = runningSeasonLookup.findRunningSeason()
                .orElseThrow(() -> new BadRequestException("noActiveSeasonWasFounded"));
        return toSeasonDto(season);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public ActiveSeasonIdDto getActiveSeasonId() {
        Season season = runningSeasonLookup.findRunningSeason()
                .orElseThrow(() -> new BadRequestException("noActiveSeasonWasFounded"));
        return new ActiveSeasonIdDto(season.getId());
    }

    // ------------------------------------------------------------------------------------------------------ //


    public SeasonDto getScheduledSeason() {
        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.SCHEDULED);
        if (seasonByStatus.isEmpty()) {
            throw new BadRequestException("noScheduledSeasonWasFounded");
        }
        Season season = seasonByStatus.get();
        return toSeasonDto(season);
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
        if (season.getStatus() != Season.Status.SCHEDULED) {
            throw new ConflictException("seasonRegistrationClosed");
        }
        if (season.getPlayers().stream().anyMatch(player -> player.getId().equals(userId))) {
            throw new ConflictException("youAlreadyRegisteredInSeason");
        }

        season.getPlayers().add(user);
        seasonsRepository.save(season);
        return toSeasonDto(season);
    }

    // ------------------------------------------------------------------------------------------------------ //


    public LeaguesPage getLeaguesBySeason(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        List<LeagueDto> leagues = season.getLeagues() == null
                ? List.of()
                : season.getLeagues().stream().map(l -> toLeagueDto(season, l)).toList();
        return LeaguesPage.builder()
                .leagues(leagues)
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

        String formatId = newLeague.getTournamentFormatId();
        if (!tournamentFormatsRepository.existsById(formatId)) {
            throw new NotFoundException("TournamentFormat", formatId);
        }

        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(formatId);
        String firstSlotId = tournamentFormatExpander.expand(format).stream()
                .findFirst()
                .map(ExpandedMatchdaySlot::getId)
                .orElse("1");

        League league = League.builder()
                .createdAt(Instant.now())
                .leagueCode(League.LeagueCode.valueOf(newLeague.getLeagueCode()))
                .name(newLeague.getLeagueCode() + " " + season.getTitle())
                .currentMatchDay(firstSlotId)
                .tournamentFormatId(formatId)
                .teams(new ArrayList<>())
                .build();

        leaguesRepository.save(league);
        season.getLeagues().add(league);
        seasonsRepository.save(season);

        return toSeasonDto(season);
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

    @Transactional
    public TeamDto removeTeamFromLeagueInSeason(String seasonId, String leagueId, String teamId) {
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

        League leagueInSeason = season.getLeagues().stream()
                .filter(l -> l.getId().equals(leagueId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("League", leagueId));

        boolean removed = leagueInSeason.getTeams().removeIf(t -> t.getId().equals(teamId));
        if (!removed) {
            throw new BadRequestException("teamNotInLeagueInThisSeason");
        }

        leaguesRepository.save(leagueInSeason);

        return TeamDto.from(team);
    }

    @Transactional
    public SeasonDto removeLeagueFromSeason(String seasonId, String leagueId) {
        if (leagueId == null || leagueId.isBlank()) {
            throw new BadRequestException("leagueIdIsNull");
        }
        if (seasonId == null || seasonId.isBlank()) {
            throw new BadRequestException("seasonIdIsNull");
        }
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        League league = season.getLeagues().stream()
                .filter(l -> l.getId().equals(leagueId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("League", leagueId));

        if (betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED) > 0) {
            throw new ConflictException("leagueHasBetsCannotRemove");
        }

        season.getLeagues().removeIf(l -> l.getId().equals(leagueId));
        seasonsRepository.save(season);
        leaguesRepository.delete(league);

        return toSeasonDto(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    private SeasonDto toSeasonDto(Season season) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate();
        List<LeagueDto> leagues = season.getLeagues() == null
                ? List.of()
                : season.getLeagues().stream()
                .map(league -> toLeagueDto(season, league))
                .collect(Collectors.toList());

        return SeasonDto.builder()
                .id(season.getId())
                .title(season.getTitle())
                .startDate(start)
                .endDate(end)
                .externalSeasonYear(SeasonCalendarUtils.resolveExternalSeasonYear(start))
                .availableExternalYears(SeasonCalendarUtils.availableExternalYears(start, end))
                .betCountPerMatchDay(season.getBetCountPerMatchDay())
                .defaultBetSize(season.getDefaultBetSize() != null ? season.getDefaultBetSize() : 10)
                .status(season.getStatus().name())
                .players(UserDto.from(season.getPlayers()))
                .leagues(leagues)
                .build();
    }

    private LeagueDto toLeagueDto(Season season, League league) {
        List<ExpandedMatchdaySlotDto> slots = leagueMatchdayService.expandSlotsForLeague(league);
        String currentForUi = season.getStatus() == Season.Status.FINISHED
                ? league.getCurrentMatchDay()
                : leagueMatchdayService.resolveEffectiveCurrentMatchDay(league);
        return LeagueDto.builder()
                .id(league.getId())
                .leagueCode(league.getLeagueCode().toString())
                .name(league.getName())
                .currentMatchDay(currentForUi)
                .tournamentFormatId(league.getTournamentFormatId())
                .matchdaySlots(slots.isEmpty() ? null : slots)
                .teams(TeamDto.from(league.getTeams()))
                .removable(betsRepository.countBetsByLeagueAndBetStatusNot(league, Bet.BetStatus.DELETED) == 0)
                .build();
    }

}
