package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.Soccer365ScheduleSyncResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.oddsapi.OddsMergedOddsService;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Soccer365ScheduleSyncService implements ScheduleProvider {

    private static final Logger log = LoggerFactory.getLogger(Soccer365ScheduleSyncService.class);

    @Override
    public String providerId() {
        return MatchDataProviders.SOCCER365;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.SCHEDULE);
    }

    private final Soccer365Properties properties;
    private final Soccer365HttpClient httpClient;
    private final Soccer365ScheduleParser scheduleParser;
    private final Soccer365TeamNamesService teamNamesService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdaySlotSupport matchdaySlotSupport;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final GetEntityService getEntityService;
    private final TeamAliasResolver teamAliasResolver;
    private final MatchScheduleRepository matchScheduleRepository;
    private final ErrorLogService errorLogService;
    private final OddsMergedOddsService oddsMergedOddsService;
    private final ExternalApiMonitoringService monitoringService;

    public Soccer365ScheduleSyncResultDto syncByLeagueCode(String leagueCodeRaw, Integer matchday) {
        League.LeagueCode leagueCode = Soccer365TeamNamesService.parseLeagueCode(leagueCodeRaw);
        Season season = runningSeasonLookup.findRunningSeasonOrThrow("noActiveSeasonWasFounded");
        League league = findLeague(season, leagueCode)
                .orElseThrow(() -> new BadRequestException("leagueNotFoundInSeason"));
        return syncLeague(season, league, false, ExternalApiMonitoringTrigger.ADMIN, matchday);
    }

    public Soccer365ScheduleSyncResultDto syncLeague(Season season, League league, boolean respectFarKickoffSkip) {
        return syncLeague(season, league, respectFarKickoffSkip, ExternalApiMonitoringTrigger.CRON, null);
    }

    public Soccer365ScheduleSyncResultDto syncLeague(
            Season season,
            League league,
            boolean respectFarKickoffSkip,
            ExternalApiMonitoringTrigger trigger
    ) {
        return syncLeague(season, league, respectFarKickoffSkip, trigger, null);
    }

    public Soccer365ScheduleSyncResultDto syncLeague(
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
        String externalSeason = matchdaySlotSupport.resolveExternalSeasonYear(season, leagueCode);
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.SCHEDULE,
                MatchDataProviders.SOCCER365,
                trigger,
                leagueCode.name(),
                externalSeason
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        try {
            return syncLeagueBody(season, league, respectFarKickoffSkip, run, httpLogs, externalSeason, explicitMatchday);
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

    private Soccer365ScheduleSyncResultDto syncLeagueBody(
            Season season,
            League league,
            boolean respectFarKickoffSkip,
            ExternalApiMonitoringRun run,
            List<ExternalApiHttpLogEntry> httpLogs,
            String externalSeason,
            Integer explicitMatchday
    ) {
        League.LeagueCode leagueCode = league.getLeagueCode();
        int competitionId = teamNamesService.requireCompetitionId(leagueCode);

        int currentOrder;
        Integer nextOrder = null;
        String currentSlotId;
        String nextSlotId = null;
        boolean includeNext;

        List<ExpandedMatchdaySlot> formatSlots = List.of();
        if (league.getTournamentFormatId() != null && !league.getTournamentFormatId().isBlank()) {
            TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
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
            Optional<Integer> currentOrderOpt = matchdaySlotSupport.resolveSlotOrder(league, league.getCurrentMatchDay());
            if (currentOrderOpt.isEmpty()) {
                throw new BadRequestException("currentMatchdayUnresolved");
            }
            currentOrder = currentOrderOpt.get();
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
                    log.info("soccer365 sync {}: next matchday after {} missing — OK", leagueCode, currentOrder);
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

        Instant reqAt = Instant.now();
        long t0 = System.currentTimeMillis();
        String html;
        try {
            html = httpClient.fetchScheduleHtml(competitionId);
            httpLogs.add(ExternalApiMonitoringService.httpLog(
                    "SCHEDULE_PAGE",
                    String.valueOf(competitionId),
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
                    String.valueOf(competitionId),
                    null,
                    "HTTP_ERROR",
                    System.currentTimeMillis() - t0,
                    e.getMessage(),
                    null,
                    reqAt
            ));
            throw e;
        }

        Soccer365ParsedSchedule parsed = scheduleParser.parse(html, competitionId);

        if (respectFarKickoffSkip && shouldSkipFarKickoff(parsed, window)) {
            log.info("soccer365 sync {}: earliest kickoff farther than {} days — skip tick",
                    leagueCode, properties.getSkipWhenKickoffFartherThanDays());
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().roundsParsed(0).skipped(1).build(),
                    httpLogs,
                    List.of(),
                    "farKickoff"
            );
            return Soccer365ScheduleSyncResultDto.builder()
                    .leagueCode(leagueCode.name())
                    .seasonId(season.getId())
                    .currentMatchday(currentOrder)
                    .nextMatchday(nextOrder)
                    .roundsParsed(0)
                    .build();
        }

        Instant fetchedAt = Instant.now();
        int upserted = 0;
        int skippedUnmapped = 0;
        Set<String> unmappedNames = new LinkedHashSet<>();
        int roundsParsed = 0;

        for (Soccer365ParsedSchedule.Round round : parsed.getRounds()) {
            if (!window.contains(round.getNumber())) {
                continue;
            }
            roundsParsed++;
            String slotId = round.getNumber() == currentOrder
                    ? currentSlotId
                    : (nextSlotId != null ? nextSlotId : String.valueOf(round.getNumber()));
            for (Soccer365ParsedSchedule.Match match : round.getMatches()) {
                Optional<Team> home = teamAliasResolver.resolveSoccer365ByName(match.getHomeName());
                Optional<Team> away = teamAliasResolver.resolveSoccer365ByName(match.getAwayName());
                if (home.isEmpty() || away.isEmpty()) {
                    skippedUnmapped++;
                    if (home.isEmpty()) {
                        unmappedNames.add(match.getHomeName());
                    }
                    if (away.isEmpty()) {
                        unmappedNames.add(match.getAwayName());
                    }
                    errorLogService.recordTeamMappingMissing(
                            MatchDataProviders.SOCCER365,
                            leagueCode.name(),
                            externalSeason,
                            round.getNumber(),
                            match.getHomeName(),
                            match.getAwayName(),
                            "soccer365TeamAliasMissing"
                    );
                    continue;
                }
                upsertMatch(
                        season.getId(),
                        league,
                        round.getNumber(),
                        slotId,
                        home.get().getId(),
                        away.get().getId(),
                        match,
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

        return Soccer365ScheduleSyncResultDto.builder()
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

    private void upsertMatch(
            String seasonId,
            League league,
            int matchday,
            String slotId,
            String homeTeamId,
            String awayTeamId,
            Soccer365ParsedSchedule.Match match,
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
        if (match.getSoccer365GameId() != null && !match.getSoccer365GameId().isBlank()) {
            existing.putExternalId(MatchDataProviders.sourcesStorageKey(MatchDataProviders.SOCCER365),
                    match.getSoccer365GameId());
        }
        existing.setFetchedAt(fetchedAt);
        matchScheduleRepository.save(existing);
        oddsMergedOddsService.deleteIfFinalized(existing);
    }

    private boolean shouldSkipFarKickoff(Soccer365ParsedSchedule parsed, Set<Integer> window) {
        int days = properties.getSkipWhenKickoffFartherThanDays();
        if (days <= 0) {
            return false;
        }
        Instant earliest = null;
        for (Soccer365ParsedSchedule.Round round : parsed.getRounds()) {
            if (!window.contains(round.getNumber())) {
                continue;
            }
            for (Soccer365ParsedSchedule.Match match : round.getMatches()) {
                if (match.getUtcKickoff() == null) {
                    continue;
                }
                if (earliest == null || match.getUtcKickoff().isBefore(earliest)) {
                    earliest = match.getUtcKickoff();
                }
            }
        }
        if (earliest == null) {
            return false;
        }
        long daysUntil = ChronoUnit.DAYS.between(Instant.now(), earliest);
        return daysUntil > days;
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
