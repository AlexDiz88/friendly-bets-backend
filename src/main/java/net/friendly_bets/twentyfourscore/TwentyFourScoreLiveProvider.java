package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.GameScore;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TwentyFourScoreLiveProvider implements LiveMatchProvider {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreLiveProvider.class);

    private final TwentyFourScoreHttpClient httpClient;
    private final TwentyFourScoreDatePageParser datePageParser;
    private final MatchScheduleRepository matchScheduleRepository;
    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;
    private final ExternalApiMonitoringService monitoringService;

    @Override
    public String providerId() {
        return ExternalProviderIds.TWENTYFOUR_SCORE;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.LIVE);
    }

    @Override
    public LiveSyncResult syncLive(Season season) {
        if (season == null || season.getId() == null || season.getId().isBlank()) {
            throw new BadRequestException("seasonRequired");
        }

        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.LIVE,
                ExternalProviderIds.TWENTYFOUR_SCORE,
                ExternalApiMonitoringTrigger.CRON,
                "ALL",
                season.getId()
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();

        Instant now = Instant.now();
        List<MatchSchedule> allSchedules = matchScheduleRepository.findBySeasonId(season.getId());
        int skippedMissingKickoff = (int) allSchedules.stream()
                .filter(TwentyFourScoreLiveSupport::isMissingUtcKickoffSkip)
                .count();

        List<MatchSchedule> tracked = allSchedules.stream()
                .filter(s -> LiveMinuteLabelResolver.isSupportedLeagueCode(s.getLeagueCode()))
                .filter(s -> TwentyFourScoreLiveSupport.isLiveHttpCandidate(s, now))
                .sorted(Comparator.comparing(MatchSchedule::getUtcKickoff, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

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

        try {
            for (LocalDate date : dates) {
                Instant reqAt = Instant.now();
                long t0 = System.currentTimeMillis();
                String html;
                try {
                    html = httpClient.fetchDateFootballHtml(date);
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

                TwentyFourScoreParsedDatePage page = datePageParser.parse(html);
                List<MatchSchedule> dateTracked = tracked.stream()
                        .filter(s -> LocalDate.ofInstant(s.getUtcKickoff(), ZoneOffset.UTC).equals(date))
                        .toList();

                for (MatchSchedule schedule : dateTracked) {
                    League.LeagueCode leagueCode = parseLeagueCode(schedule.getLeagueCode());
                    if (leagueCode == null) {
                        continue;
                    }
                    List<TwentyFourScoreParsedDatePage.MatchRow> leagueRows = collectLeagueRows(page, leagueCode);
                    Optional<TwentyFourScoreParsedDatePage.MatchRow> row = findRow(schedule, leagueRows, teamCache);
                    if (row.isEmpty()) {
                        continue;
                    }
                    boolean wasFinished = TwentyFourScoreLiveSupport.isFinishedStatus(schedule.getStatus());
                    applyLiveRow(schedule, row.get(), now);
                    matchScheduleRepository.save(schedule);
                    updated++;
                    if (!wasFinished && TwentyFourScoreLiveSupport.isFinishedStatus(schedule.getStatus())) {
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

        LinkedHashSet<String> pendingFullIds = allSchedules.stream()
                .filter(TwentyFourScoreLiveSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() != null)
                .map(MatchSchedule::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!pendingFullIds.isEmpty()) {
            log.info("24score LIVE pending FULL for {} match(es), httpRequests={}", pendingFullIds.size(), httpRequests);
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

    private static League.LeagueCode parseLeagueCode(String leagueCode) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return null;
        }
        try {
            return League.LeagueCode.valueOf(leagueCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<TwentyFourScoreParsedDatePage.MatchRow> collectLeagueRows(
            TwentyFourScoreParsedDatePage page,
            League.LeagueCode leagueCode
    ) {
        List<TwentyFourScoreParsedDatePage.MatchRow> rows = new ArrayList<>();
        for (TwentyFourScoreParsedDatePage.CompetitionBlock block : page.getCompetitions()) {
            if (TwentyFourScoreLeagueTitles.matches(leagueCode, block.getTitle())) {
                rows.addAll(block.getMatches());
            }
        }
        return rows;
    }

    private Optional<TwentyFourScoreParsedDatePage.MatchRow> findRow(
            MatchSchedule schedule,
            List<TwentyFourScoreParsedDatePage.MatchRow> rows,
            Map<String, Team> teamCache
    ) {
        Team home = resolveTeam(schedule.getHomeTeamId(), teamCache);
        Team away = resolveTeam(schedule.getAwayTeamId(), teamCache);
        if (home == null || away == null) {
            return Optional.empty();
        }
        for (TwentyFourScoreParsedDatePage.MatchRow row : rows) {
            if (teamAliasResolver.teamMatchesProviderSide(home, ExternalProviderIds.TWENTYFOUR_SCORE, row.getHomeName())
                    && teamAliasResolver.teamMatchesProviderSide(away, ExternalProviderIds.TWENTYFOUR_SCORE, row.getAwayName())) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private Team resolveTeam(String teamId, Map<String, Team> cache) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return cache.computeIfAbsent(teamId, id -> teamsRepository.findById(id).orElse(null));
    }

    private void applyLiveRow(MatchSchedule schedule, TwentyFourScoreParsedDatePage.MatchRow row, Instant now) {
        schedule.setStatus(row.getStatus());
        if (row.getLiveMinuteLabel() != null && !row.getLiveMinuteLabel().isBlank()) {
            String resolvedLabel = LiveMinuteLabelResolver.resolve(
                    row.getLiveMinuteLabel(),
                    schedule.getUtcKickoff(),
                    now
            );
            schedule.setLiveMinuteLabel(resolvedLabel);
            Integer minute = LiveMinuteLabelResolver.parseMinuteInteger(row.getLiveMinuteLabel());
            if (minute != null) {
                schedule.setLiveMinute(minute);
            }
        } else if (TwentyFourScoreLiveSupport.isFinishedStatus(row.getStatus())) {
            schedule.setLiveMinute(null);
            schedule.setLiveMinuteLabel(null);
        }
        if (row.getFullTimeScore() != null) {
            GameScore score = schedule.getGameScore() != null ? schedule.getGameScore() : new GameScore();
            score.setFullTime(row.getFullTimeScore());
            if (row.getFirstTimeScore() != null) {
                score.setFirstTime(row.getFirstTimeScore());
            }
            schedule.setGameScore(score);
        }
        schedule.setFetchedAt(Instant.now());
    }
}
