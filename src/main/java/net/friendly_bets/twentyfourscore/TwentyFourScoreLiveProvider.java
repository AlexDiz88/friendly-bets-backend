package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataProvider;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        return MatchDataProviders.TWENTYFOUR_SCORE;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
        return ExternalDataProvider.of(ExternalDataLayer.LIVE);
    }

    @Override
    public LiveSyncResult syncLeagueLive(Season season, League league) {
        if (season == null || league == null || league.getLeagueCode() == null || league.getId() == null) {
            throw new BadRequestException("leagueCodeRequired");
        }
        String leagueCode = league.getLeagueCode().name();
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.LIVE,
                MatchDataProviders.TWENTYFOUR_SCORE,
                ExternalApiMonitoringTrigger.CRON,
                leagueCode,
                season.getId()
        );
        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();

        if (!TwentyFourScoreLeagueTitles.supported().contains(league.getLeagueCode())) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().requested(0).updated(0).build(),
                    httpLogs,
                    List.of(),
                    "leagueNotSupported"
            );
            return LiveSyncResult.of(leagueCode, 0, 0, "leagueNotSupported");
        }

        Instant now = Instant.now();
        List<MatchSchedule> leagueSchedules = matchScheduleRepository.findByLeagueIdAndSeasonId(
                league.getId(), season.getId());
        int skippedMissingKickoff = (int) leagueSchedules.stream()
                .filter(s -> TwentyFourScoreLiveSupport.isMissingUtcKickoffSkip(s)
                        || (TwentyFourScoreLiveSupport.needsFullMatch(s) && s.getUtcKickoff() == null))
                .count();
        List<MatchSchedule> candidates = leagueSchedules.stream()
                .filter(s -> TwentyFourScoreLiveSupport.isLiveHttpCandidate(s, now))
                .toList();
        if (candidates.isEmpty()) {
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
            return LiveSyncResult.of(leagueCode, 0, 0, skipReason);
        }

        Set<LocalDate> dates = new HashSet<>();
        for (MatchSchedule schedule : candidates) {
            dates.add(LocalDate.ofInstant(schedule.getUtcKickoff(), ZoneOffset.UTC));
        }

        Map<String, Team> teamCache = new HashMap<>();
        int updated = 0;
        int finishedDetected = 0;

        try {
            for (LocalDate date : dates) {
                Instant reqAt = Instant.now();
                long t0 = System.currentTimeMillis();
                String html;
                try {
                    html = httpClient.fetchDateFootballHtml(date);
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
                List<TwentyFourScoreParsedDatePage.MatchRow> rows = new ArrayList<>();
                for (TwentyFourScoreParsedDatePage.CompetitionBlock block : page.getCompetitions()) {
                    if (TwentyFourScoreLeagueTitles.matches(league.getLeagueCode(), block.getTitle())) {
                        rows.addAll(block.getMatches());
                    }
                }
                for (MatchSchedule schedule : candidates) {
                    Optional<TwentyFourScoreParsedDatePage.MatchRow> row = findRow(schedule, rows, teamCache);
                    if (row.isEmpty()) {
                        continue;
                    }
                    boolean wasFinished = TwentyFourScoreLiveSupport.isFinishedStatus(schedule.getStatus());
                    applyLiveRow(schedule, row.get());
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
                            .requested(candidates.size())
                            .updated(updated)
                            .finishedDetected(finishedDetected)
                            .build(),
                    httpLogs,
                    List.of(),
                    e.getMessage()
            );
            throw e;
        }

        LinkedHashSet<String> pendingFullIds = candidates.stream()
                .filter(TwentyFourScoreLiveSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() != null)
                .map(MatchSchedule::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        leagueSchedules.stream()
                .filter(TwentyFourScoreLiveSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() != null)
                .map(MatchSchedule::getId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(pendingFullIds::add);

        int missingKickoffForFull = (int) leagueSchedules.stream()
                .filter(TwentyFourScoreLiveSupport::needsFullMatch)
                .filter(s -> s.getUtcKickoff() == null)
                .count();
        int totalMissingKickoff = (int) leagueSchedules.stream()
                .filter(TwentyFourScoreLiveSupport::isMissingUtcKickoffSkip)
                .count() + missingKickoffForFull;

        if (!pendingFullIds.isEmpty()) {
            log.info("24score LIVE {} pending FULL for {} match(es)", league.getLeagueCode(), pendingFullIds.size());
        }

        String warning = totalMissingKickoff > 0
                ? ExternalApiMonitoringService.reasonMissingUtcKickoff(totalMissingKickoff)
                : null;
        monitoringService.finalizeAndSave(
                run,
                ExternalApiMonitoringStatus.SUCCESS,
                ExternalApiMonitoringCounters.builder()
                        .requested(candidates.size())
                        .updated(updated)
                        .finishedDetected(finishedDetected)
                        .skipped(totalMissingKickoff)
                        .build(),
                httpLogs,
                List.of(),
                warning
        );

        return new LiveSyncResult(leagueCode, updated, finishedDetected, warning, List.copyOf(pendingFullIds));
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
            if (teamAliasResolver.teamMatchesScoreProviderSide(home, MatchDataProviders.TWENTYFOUR_SCORE, row.getHomeName())
                    && teamAliasResolver.teamMatchesScoreProviderSide(away, MatchDataProviders.TWENTYFOUR_SCORE, row.getAwayName())) {
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

    private void applyLiveRow(MatchSchedule schedule, TwentyFourScoreParsedDatePage.MatchRow row) {
        schedule.setStatus(row.getStatus());
        schedule.setLiveMinuteLabel(row.getLiveMinuteLabel());
        if (row.getLiveMinuteLabel() != null) {
            String digits = row.getLiveMinuteLabel().replaceAll("\\D+", "");
            if (!digits.isBlank()) {
                try {
                    schedule.setLiveMinute(Integer.parseInt(digits));
                } catch (NumberFormatException ignored) {
                    // keep previous
                }
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
