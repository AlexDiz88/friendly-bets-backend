package net.friendly_bets.sportsru;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ScheduleSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.matchschedule.MatchScheduleCurrentSlotResolver;
import net.friendly_bets.matchschedule.ScheduleFarKickoffSkipSupport;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.OddsService;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.MatchSchedulesUpdatedEvent;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.soccer365.Soccer365TeamNamesService;
import net.friendly_bets.sportsru.config.SportsRuProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsRuScheduleSyncService implements ScheduleProvider {

    private static final Logger log = LoggerFactory.getLogger(SportsRuScheduleSyncService.class);

    @Override
    public String providerId() {
        return ExternalProviderIds.SPORTS_RU;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.SCHEDULE);
    }

    private final SportsRuProperties properties;
    private final SportsRuHttpClient httpClient;
    private final SportsRuScheduleParser scheduleParser;
    private final SportsRuTeamNamesService teamNamesService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySlotSupport;
    private final MatchScheduleCurrentSlotResolver matchScheduleCurrentSlotResolver;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final GetEntityService getEntityService;
    private final TeamAliasResolver teamAliasResolver;
    private final MatchScheduleRepository matchScheduleRepository;
    private final ErrorLogService errorLogService;
    private final OddsService oddsService;
    private final ExternalApiMonitoringService monitoringService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleFarKickoffSkipSupport farKickoffSkipSupport;

    @Override
    public ScheduleSyncResultDto syncByLeagueCode(String leagueCodeRaw, Integer matchday) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = findLeague(season, leagueCode)
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        return syncLeague(season, league, false, ExternalApiMonitoringTrigger.ADMIN, matchday);
    }

    @Override
    public ScheduleSyncResultDto syncLeague(Season season, League league, boolean respectFarKickoffSkip) {
        return syncLeague(season, league, respectFarKickoffSkip, ExternalApiMonitoringTrigger.CRON, null);
    }

    public ScheduleSyncResultDto syncLeague(
            Season season,
            League league,
            boolean respectFarKickoffSkip,
            ExternalApiMonitoringTrigger trigger,
            Integer explicitMatchday
    ) {
        if (league == null || league.getLeagueCode() == null) {
            throw new BadRequestException("leagueCodeRequired");
        }
        League.LeagueCode leagueCode = league.getLeagueCode();
        teamNamesService.requireScheduleSyncSupported(leagueCode);
        String externalSeason = matchdaySlotSupport.resolveExternalSeasonYear(season, leagueCode);
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.SCHEDULE,
                ExternalProviderIds.SPORTS_RU,
                trigger,
                leagueCode.name(),
                externalSeason
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        try {
            ScheduleSyncResultDto result = syncLeagueBody(
                    season, league, respectFarKickoffSkip, run, httpLogs, externalSeason, explicitMatchday);
            eventPublisher.publishEvent(new MatchSchedulesUpdatedEvent());
            return result;
        } catch (RuntimeException e) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder().build(),
                    httpLogs,
                    List.of(),
                    e.getMessage()
            );
            throw e;
        }
    }

    private ScheduleSyncResultDto syncLeagueBody(
            Season season,
            League league,
            boolean respectFarKickoffSkip,
            ExternalApiMonitoringRun run,
            List<ExternalApiHttpLogEntry> httpLogs,
            String externalSeason,
            Integer explicitMatchday
    ) {
        League.LeagueCode leagueCode = league.getLeagueCode();
        String calendarPath = teamNamesService.requireCalendarPath(leagueCode);

        int currentOrder;
        Integer nextOrder = null;
        String currentSlotId;
        String nextSlotId = null;
        boolean includeNext;

        TournamentFormat format = null;
        List<ExpandedMatchdaySlot> formatSlots = List.of();
        if (league.getTournamentFormatId() != null && !league.getTournamentFormatId().isBlank()) {
            format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
            formatSlots = tournamentFormatExpander.expand(format);
        }

        if (explicitMatchday != null) {
            if (explicitMatchday < 1) {
                throw new BadRequestException("invalidMatchday");
            }
            if (!formatSlots.isEmpty() && formatSlots.stream().noneMatch(s -> s.getOrder() == explicitMatchday)) {
                throw new BadRequestException("invalidMatchday");
            }
            currentOrder = explicitMatchday;
            includeNext = false;
            currentSlotId = formatSlots.stream()
                    .filter(s -> s.getOrder() == currentOrder)
                    .findFirst()
                    .map(ExpandedMatchdaySlot::getId)
                    .orElse(String.valueOf(currentOrder));
        } else {
            currentOrder = resolvePlayingCurrentOrder(league, format, externalSeason);
            includeNext = true;
            if (!formatSlots.isEmpty()) {
                Optional<ExpandedMatchdaySlot> currentSlot = formatSlots.stream()
                        .filter(s -> s.getOrder() == currentOrder)
                        .findFirst();
                currentSlotId = currentSlot.map(ExpandedMatchdaySlot::getId).orElse(String.valueOf(currentOrder));
                Optional<ExpandedMatchdaySlot> nextSlot = formatSlots.stream()
                        .filter(s -> s.getOrder() == currentOrder + 1)
                        .findFirst();
                if (nextSlot.isPresent()) {
                    nextOrder = nextSlot.get().getOrder();
                    nextSlotId = nextSlot.get().getId();
                } else {
                    log.info("sports.ru sync {}: next matchday after {} missing — OK", leagueCode, currentOrder);
                }
            } else {
                currentSlotId = String.valueOf(currentOrder);
                nextOrder = currentOrder + 1;
                nextSlotId = String.valueOf(nextOrder);
            }
        }

        Set<Integer> window = new LinkedHashSet<>();
        window.add(currentOrder);
        if (includeNext && nextOrder != null) {
            window.add(nextOrder);
        }

        run.setMatchday(currentOrder);
        run.setSlotOrders(new ArrayList<>(window));

        if (respectFarKickoffSkip && farKickoffSkipSupport.shouldSkip(
                season.getId(), league.getId(), currentOrder, window, properties.getSkipWhenKickoffFartherThanDays())) {
            log.info("sports.ru sync {}: DB data present and earliest kickoff farther than {} days — skip without HTTP",
                    leagueCode, properties.getSkipWhenKickoffFartherThanDays());
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().roundsParsed(0).skipped(1).build(),
                    httpLogs,
                    List.of(),
                    "farKickoffDb"
            );
            return ScheduleSyncResultDto.builder()
                    .leagueCode(leagueCode.name())
                    .seasonId(season.getId())
                    .currentMatchday(currentOrder)
                    .nextMatchday(nextOrder)
                    .roundsParsed(0)
                    .build();
        }

        Instant reqAt = Instant.now();
        long t0 = System.currentTimeMillis();
        String calendarHtml;
        try {
            calendarHtml = httpClient.fetchCalendarHtml(calendarPath);
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "SCHEDULE_PAGE",
                    calendarPath,
                    200,
                    "SUCCESS",
                    System.currentTimeMillis() - t0,
                    null,
                    null,
                    reqAt
            ));
        } catch (RuntimeException e) {
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "SCHEDULE_PAGE",
                    calendarPath,
                    null,
                    "HTTP_ERROR",
                    System.currentTimeMillis() - t0,
                    e.getMessage(),
                    null,
                    reqAt
            ));
            throw e;
        }

        SportsRuParsedSchedule parsed = scheduleParser.parseCalendar(calendarHtml);

        Instant fetchedAt = Instant.now();
        int upserted = 0;
        int skippedUnmapped = 0;
        Set<String> unmappedNames = new LinkedHashSet<>();
        int roundsParsed = 0;

        for (SportsRuParsedSchedule.Round round : parsed.getRounds()) {
            if (!window.contains(round.getNumber())) {
                continue;
            }
            roundsParsed++;
            String slotId = round.getNumber() == currentOrder
                    ? currentSlotId
                    : (nextSlotId != null ? nextSlotId : String.valueOf(round.getNumber()));
            for (SportsRuParsedSchedule.Match match : round.getMatches()) {
                Optional<Team> home = teamAliasResolver.resolveByProviderName(
                        ExternalProviderIds.SPORTS_RU, match.getHomeName());
                Optional<Team> away = teamAliasResolver.resolveByProviderName(
                        ExternalProviderIds.SPORTS_RU, match.getAwayName());
                if (home.isEmpty() || away.isEmpty()) {
                    skippedUnmapped++;
                    if (home.isEmpty()) {
                        unmappedNames.add(match.getHomeName());
                    }
                    if (away.isEmpty()) {
                        unmappedNames.add(match.getAwayName());
                    }
                    errorLogService.recordTeamMappingMissing(
                            ExternalProviderIds.SPORTS_RU,
                            leagueCode.name(),
                            externalSeason,
                            round.getNumber(),
                            match.getHomeName(),
                            match.getAwayName(),
                            "sportsRuTeamAliasMissing"
                    );
                    continue;
                }

                Instant utcKickoff = fetchMatchKickoff(match.getMatchPath(), httpLogs);
                SportsRuParsedSchedule.Match enriched = SportsRuParsedSchedule.Match.builder()
                        .homeName(match.getHomeName())
                        .awayName(match.getAwayName())
                        .matchPath(match.getMatchPath())
                        .utcKickoff(utcKickoff)
                        .status(match.getStatus())
                        .build();

                upsertMatch(
                        season.getId(),
                        league,
                        round.getNumber(),
                        slotId,
                        home.get().getId(),
                        away.get().getId(),
                        enriched,
                        fetchedAt
                );
                upserted++;
            }
        }

        ExternalApiMonitoringStatus status = skippedUnmapped > 0
                ? ExternalApiMonitoringStatus.PARTIAL
                : ExternalApiMonitoringStatus.SUCCESS;
        monitoringService.finalizeAndSave(
                run,
                status,
                ExternalApiMonitoringCounters.builder()
                        .upserted(upserted)
                        .skipped(skippedUnmapped)
                        .roundsParsed(roundsParsed)
                        .mappingFailures(skippedUnmapped)
                        .build(),
                httpLogs,
                List.of(),
                skippedUnmapped > 0 ? "skippedUnmapped=" + skippedUnmapped : null
        );

        return ScheduleSyncResultDto.builder()
                .leagueCode(leagueCode.name())
                .seasonId(season.getId())
                .currentMatchday(currentOrder)
                .nextMatchday(nextOrder)
                .upserted(upserted)
                .skippedUnmapped(skippedUnmapped)
                .roundsParsed(roundsParsed)
                .unmappedNames(new ArrayList<>(unmappedNames))
                .build();
    }

    private Instant fetchMatchKickoff(String matchPath, List<ExternalApiHttpLogEntry> httpLogs) {
        Instant reqAt = Instant.now();
        long t0 = System.currentTimeMillis();
        try {
            String html = httpClient.fetchMatchHtml(matchPath);
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "MATCH_PAGE",
                    matchPath,
                    200,
                    "SUCCESS",
                    System.currentTimeMillis() - t0,
                    null,
                    null,
                    reqAt
            ));
            return scheduleParser.parseUtcKickoffFromMatchHtml(html);
        } catch (RuntimeException e) {
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "MATCH_PAGE",
                    matchPath,
                    null,
                    "HTTP_ERROR",
                    System.currentTimeMillis() - t0,
                    e.getMessage(),
                    null,
                    reqAt
            ));
            throw e;
        }
    }

    private void upsertMatch(
            String seasonId,
            League league,
            int matchday,
            String slotId,
            String homeTeamId,
            String awayTeamId,
            SportsRuParsedSchedule.Match match,
            Instant fetchedAt
    ) {
        MatchSchedule existing = matchScheduleRepository
                .findByLeagueIdAndSeasonIdAndMatchdayAndHomeTeamIdAndAwayTeamId(
                        league.getId(), seasonId, matchday, homeTeamId, awayTeamId)
                .orElse(null);
        if (existing == null) {
            existing = MatchSchedule.builder()
                    .seasonId(seasonId)
                    .leagueId(league.getId())
                    .leagueCode(league.getLeagueCode().name())
                    .matchday(matchday)
                    .slotId(slotId)
                    .homeTeamId(homeTeamId)
                    .awayTeamId(awayTeamId)
                    .gameScore(null)
                    .build();
        }
        existing.setSlotId(slotId);
        existing.setLeagueCode(league.getLeagueCode().name());
        if (match.getUtcKickoff() != null) {
            existing.setUtcKickoff(match.getUtcKickoff());
        }
        if (match.getStatus() != null) {
            existing.setStatus(match.getStatus());
        }
        existing.setFetchedAt(fetchedAt);
        matchScheduleRepository.save(existing);
        oddsService.deleteIfFinalized(existing);
    }

    private int resolvePlayingCurrentOrder(League league, TournamentFormat format, String externalSeason) {
        if (format != null) {
            return matchScheduleCurrentSlotResolver.resolveCurrentSlotOrder(league, format, externalSeason);
        }
        return matchdaySlotSupport.resolveSlotOrder(league, league.getCurrentMatchDay())
                .orElseThrow(() -> new BadRequestException("currentMatchdayUnresolved"));
    }

    private Optional<League> findLeague(Season season, League.LeagueCode leagueCode) {
        if (season.getLeagues() == null) {
            return Optional.empty();
        }
        return season.getLeagues().stream()
                .filter(l -> l != null && l.getLeagueCode() == leagueCode)
                .findFirst();
    }
}
