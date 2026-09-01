package net.friendly_bets.eurofootball;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.providers.live.LiveMatchApplySupport;
import net.friendly_bets.providers.live.LiveMatchSupport;
import net.friendly_bets.providers.live.LiveMatchSyncDiagnostics;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.TeamAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EuroFootballLiveProvider implements LiveMatchProvider {

    private static final Logger log = LoggerFactory.getLogger(EuroFootballLiveProvider.class);

    private final EuroFootballHttpClient httpClient;
    private final EuroFootballDateHtmlParser dateHtmlParser;
    private final MatchScheduleRepository matchScheduleRepository;
    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;
    private final ExternalApiMonitoringService monitoringService;
    private final LiveMatchSyncDiagnostics liveMatchSyncDiagnostics;

    @Override
    public String providerId() {
        return ExternalProviderIds.EURO_FOOTBALL;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.LIVE);
    }

    @Override
    public LiveSyncResult syncLive(Season season) {
        return syncLive(season, null);
    }

    @Override
    public LiveSyncResult syncLive(Season season, LocalDate utcDate) {
        if (season == null || season.getId() == null || season.getId().isBlank()) {
            throw new BadRequestException("seasonRequired");
        }

        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.LIVE,
                ExternalProviderIds.EURO_FOOTBALL,
                ExternalApiMonitoringTrigger.CRON,
                utcDate != null ? utcDate.toString() : "ALL",
                season.getId()
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();

        Instant now = Instant.now();
        List<MatchSchedule> allSchedules = matchScheduleRepository.findBySeasonId(season.getId());
        int skippedMissingKickoff = (int) allSchedules.stream()
                .filter(LiveMatchSupport::isMissingUtcKickoffSkip)
                .count();

        List<MatchSchedule> tracked = allSchedules.stream()
                .filter(s -> EuroFootballLeagueSupport.isSupportedLeagueCode(s.getLeagueCode()))
                .filter(s -> LiveMatchSupport.isLiveHttpCandidate(s, now))
                .sorted(Comparator.comparing(MatchSchedule::getUtcKickoff, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (utcDate != null) {
            tracked = tracked.stream()
                    .filter(s -> LocalDate.ofInstant(s.getUtcKickoff(), ZoneOffset.UTC).equals(utcDate))
                    .toList();
        }

        if (tracked.isEmpty()) {
            String skipReason = skippedMissingKickoff > 0
                    ? ExternalApiMonitoringService.reasonMissingUtcKickoff(skippedMissingKickoff)
                    : "noLiveCandidates";
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder()
                            .requested(0)
                            .updated(0)
                            .skipped(skippedMissingKickoff)
                            .build(),
                    httpLogs,
                    List.of(),
                    skipReason
            );
            return LiveSyncResult.skipped(skipReason);
        }

        Set<LocalDate> dates = tracked.stream()
                .map(s -> LocalDate.ofInstant(s.getUtcKickoff(), ZoneOffset.UTC))
                .collect(Collectors.toCollection(TreeSet::new));

        Map<String, Team> teamCache = new HashMap<>();
        int httpRequests = 0;
        int updated = 0;
        int finishedDetected = 0;
        Set<String> updatedIds = new HashSet<>();
        Set<String> notFoundIds = new HashSet<>();

        try {
            for (LocalDate date : dates) {
                Instant reqAt = Instant.now();
                long t0 = System.currentTimeMillis();
                String html;
                try {
                    html = httpClient.fetchDateOnlineHtml(date);
                    httpRequests++;
                    httpLogs.add(ExternalApiMonitoringService.httpLog(
                            "DATE_PAGE",
                            date.toString(),
                            200,
                            "SUCCESS",
                            System.currentTimeMillis() - t0,
                            null,
                            null,
                            reqAt
                    ));
                } catch (RuntimeException e) {
                    httpLogs.add(ExternalApiMonitoringService.httpLog(
                            "DATE_PAGE",
                            date.toString(),
                            null,
                            "HTTP_ERROR",
                            System.currentTimeMillis() - t0,
                            e.getMessage(),
                            null,
                            reqAt
                    ));
                    throw e;
                }

                EuroFootballParsedDatePage page = dateHtmlParser.parse(html);

                List<MatchSchedule> dateTracked = tracked.stream()
                        .filter(s -> LocalDate.ofInstant(s.getUtcKickoff(), ZoneOffset.UTC).equals(date))
                        .toList();

                for (MatchSchedule schedule : dateTracked) {
                    League.LeagueCode leagueCode = EuroFootballLeagueSupport.parseLeagueCode(schedule.getLeagueCode());
                    if (leagueCode == null) {
                        continue;
                    }
                    List<EuroFootballParsedDatePage.MatchRow> leagueRows = collectLeagueRows(page, leagueCode);
                    Optional<EuroFootballParsedDatePage.MatchRow> row = findRow(
                            schedule,
                            leagueRows,
                            teamCache,
                            season.getId()
                    );
                    if (row.isEmpty()) {
                        notFoundIds.add(schedule.getId());
                        continue;
                    }
                    boolean wasFinished = LiveMatchSupport.isFinishedStatus(schedule.getStatus());
                    LiveMatchApplySupport.apply(schedule, row.get().getSnapshot(), now);
                    matchScheduleRepository.save(schedule);
                    updated++;
                    updatedIds.add(schedule.getId());
                    if (!wasFinished && LiveMatchSupport.isFinishedStatus(schedule.getStatus())) {
                        finishedDetected++;
                    }
                }
            }
        } catch (RuntimeException e) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.FAILED,
                    ExternalApiMonitoringCounters.builder()
                            .requested(tracked.size())
                            .updated(updated)
                            .finishedDetected(finishedDetected)
                            .build(),
                    httpLogs,
                    List.of(),
                    e.getMessage()
            );
            throw e;
        }

        liveMatchSyncDiagnostics.afterSync(
                ExternalProviderIds.EURO_FOOTBALL,
                season.getId(),
                tracked,
                updatedIds,
                notFoundIds,
                now
        );

        LinkedHashSet<String> pendingFullIds = allSchedules.stream()
                .filter(LiveMatchSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() != null)
                .map(MatchSchedule::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!pendingFullIds.isEmpty()) {
            log.info("euro-football LIVE pending FULL for {} match(es), httpRequests={}",
                    pendingFullIds.size(), httpRequests);
        }

        String warning = skippedMissingKickoff > 0
                ? ExternalApiMonitoringService.reasonMissingUtcKickoff(skippedMissingKickoff)
                : null;
        monitoringService.finalizeAndSave(
                run,
                ExternalApiMonitoringStatus.SUCCESS,
                ExternalApiMonitoringCounters.builder()
                        .requested(tracked.size())
                        .updated(updated)
                        .finishedDetected(finishedDetected)
                        .skipped(skippedMissingKickoff)
                        .build(),
                httpLogs,
                List.of(),
                warning
        );

        List<String> datesSynced = dates.stream().map(LocalDate::toString).toList();
        return new LiveSyncResult(
                httpRequests,
                tracked.size(),
                updated,
                finishedDetected,
                warning,
                datesSynced,
                List.copyOf(pendingFullIds)
        );
    }

    private static List<EuroFootballParsedDatePage.MatchRow> collectLeagueRows(
            EuroFootballParsedDatePage page,
            League.LeagueCode leagueCode
    ) {
        List<EuroFootballParsedDatePage.MatchRow> rows = new ArrayList<>();
        for (EuroFootballParsedDatePage.CompetitionBlock block : page.getCompetitions()) {
            if (EuroFootballLeagueSupport.matchesTournament(leagueCode, block.getSlug(), block.getParentSlug())) {
                rows.addAll(block.getMatches());
            }
        }
        return rows;
    }

    private Optional<EuroFootballParsedDatePage.MatchRow> findRow(
            MatchSchedule schedule,
            List<EuroFootballParsedDatePage.MatchRow> rows,
            Map<String, Team> teamCache,
            String seasonId
    ) {
        Team home = resolveTeam(schedule.getHomeTeamId(), teamCache);
        Team away = resolveTeam(schedule.getAwayTeamId(), teamCache);
        if (home == null || away == null) {
            return Optional.empty();
        }
        List<EuroFootballParsedDatePage.MatchRow> matches = new ArrayList<>();
        for (EuroFootballParsedDatePage.MatchRow row : rows) {
            if (teamAliasResolver.teamMatchesProviderSide(home, ExternalProviderIds.EURO_FOOTBALL, row.getHomeName())
                    && teamAliasResolver.teamMatchesProviderSide(away, ExternalProviderIds.EURO_FOOTBALL, row.getAwayName())) {
                matches.add(row);
            }
        }
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            liveMatchSyncDiagnostics.recordAmbiguous(
                    ExternalProviderIds.EURO_FOOTBALL,
                    seasonId,
                    schedule,
                    matches.size()
            );
            return matches.stream()
                    .filter(r -> LiveMatchSupport.isFinishedStatus(r.getSnapshot().status()))
                    .findFirst()
                    .or(() -> Optional.of(matches.get(0)));
        }
        return Optional.of(matches.get(0));
    }

    private Team resolveTeam(String teamId, Map<String, Team> cache) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(teamId, id -> teamsRepository.findById(id).orElse(null));
    }
}
