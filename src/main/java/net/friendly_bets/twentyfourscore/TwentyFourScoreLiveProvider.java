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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TwentyFourScoreLiveProvider implements LiveMatchProvider {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreLiveProvider.class);
    private static final Set<String> FINISHED = Set.of("FINISHED", "AWARDED", "COMPLETED", "FT", "AET", "PEN");

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

        List<MatchSchedule> candidates = matchScheduleRepository.findByLeagueIdAndSeasonId(league.getId(), season.getId())
                .stream()
                .filter(this::isLiveCandidate)
                .toList();
        if (candidates.isEmpty()) {
            monitoringService.finalizeAndSave(
                    run,
                    ExternalApiMonitoringStatus.SKIPPED,
                    ExternalApiMonitoringCounters.builder().requested(0).updated(0).build(),
                    httpLogs,
                    List.of(),
                    "noLiveCandidates"
            );
            return LiveSyncResult.of(leagueCode, 0, 0, "noLiveCandidates");
        }

        Set<LocalDate> dates = new HashSet<>();
        for (MatchSchedule schedule : candidates) {
            if (schedule.getUtcKickoff() != null) {
                dates.add(LocalDate.ofInstant(schedule.getUtcKickoff(), ZoneOffset.UTC));
            }
        }
        if (dates.isEmpty()) {
            dates.add(LocalDate.now(ZoneOffset.UTC));
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
                    boolean wasFinished = isFinishedStatus(schedule.getStatus());
                    applyLiveRow(schedule, row.get());
                    matchScheduleRepository.save(schedule);
                    updated++;
                    if (!wasFinished && isFinishedStatus(schedule.getStatus())) {
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

        List<String> pendingFullIds = candidates.stream()
                .filter(s -> isFinishedStatus(s.getStatus()) && s.getFullDetailsFetchedAt() == null)
                .map(MatchSchedule::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        if (!pendingFullIds.isEmpty()) {
            log.info("24score LIVE {} pending FULL for {} match(es)", league.getLeagueCode(), pendingFullIds.size());
        }

        monitoringService.finalizeAndSave(
                run,
                ExternalApiMonitoringStatus.SUCCESS,
                ExternalApiMonitoringCounters.builder()
                        .requested(candidates.size())
                        .updated(updated)
                        .finishedDetected(finishedDetected)
                        .build(),
                httpLogs,
                List.of(),
                null
        );

        return new LiveSyncResult(leagueCode, updated, finishedDetected, null, pendingFullIds);
    }

    private boolean isLiveCandidate(MatchSchedule schedule) {
        if (schedule == null) {
            return false;
        }
        if (schedule.getFinalizedAt() != null || schedule.getFullDetailsFetchedAt() != null) {
            return false;
        }
        String status = normalize(schedule.getStatus());
        if (FINISHED.contains(status)) {
            return true; // pending FULL
        }
        if ("LIVE".equals(status) || "IN_PLAY".equals(status) || "PAUSED".equals(status) || "HALFTIME".equals(status)) {
            return true;
        }
        Instant kickoff = schedule.getUtcKickoff();
        if (kickoff == null) {
            return false;
        }
        Instant now = Instant.now();
        return !kickoff.isAfter(now) && kickoff.isAfter(now.minusSeconds(4 * 3600L));
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
        } else if (isFinishedStatus(row.getStatus())) {
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
        if (row.getExternalMatchId() != null) {
            schedule.putExternalId(
                    MatchDataProviders.sourcesStorageKey(MatchDataProviders.TWENTYFOUR_SCORE),
                    row.getExternalMatchId()
            );
        }
        schedule.setFetchedAt(Instant.now());
    }

    private static boolean isFinishedStatus(String status) {
        return FINISHED.contains(normalize(status));
    }

    private static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
