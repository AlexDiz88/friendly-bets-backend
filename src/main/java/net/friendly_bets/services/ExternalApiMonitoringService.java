package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalApiMonitoringLayerPageDto;
import net.friendly_bets.dto.ExternalApiMonitoringRunDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.monitoring.ExternalApiMatchTeamsRef;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.ExternalApiMonitoringRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.TeamsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExternalApiMonitoringService {

    /** Soft warning key: matches skipped because {@code match_schedules.utc_kickoff} is null. */
    public static final String REASON_MISSING_UTC_KICKOFF = "missingUtcKickoff";

    /**
     * ODDS cron planner soft skips (no bookmaker HTTP). Must stay SKIPPED, not FAILED,
     * even when {@code tournamentFetched=false}.
     */
    public static final Set<String> ODDS_CRON_SOFT_SKIP_REASONS = Set.of(
            "noSlots",
            "noSseEligible",
            "noCurrentMatches",
            "farBothComplete",
            "farCurrentCompleteNoNext",
            "currentExhaustedNoNext",
            "currentExhaustedNextComplete",
            "invalidInput"
    );

    private static final ThreadLocal<ExternalApiMonitoringTrigger> TRIGGER_OVERRIDE = new ThreadLocal<>();

    public static String reasonMissingUtcKickoff(int count) {
        return REASON_MISSING_UTC_KICKOFF + "=" + Math.max(0, count);
    }

    public static boolean isOddsCronSoftSkip(String errorSummary) {
        if (errorSummary == null || errorSummary.isBlank()) {
            return false;
        }
        String first = errorSummary.split(";", 2)[0].trim();
        int eq = first.indexOf('=');
        String key = eq > 0 ? first.substring(0, eq) : first;
        int bracket = key.indexOf('[');
        if (bracket > 0) {
            key = key.substring(0, bracket).trim();
        }
        return ODDS_CRON_SOFT_SKIP_REASONS.contains(key);
    }

    /**
     * Builds {@code mappingFailures=N} or {@code mappingFailures=N [Home - Away, ...]}.
     * Labels use commas so {@code ;}-split of error summaries stays intact.
     */
    public static String mappingFailuresSummary(int count, List<String> labels) {
        int safe = Math.max(0, count);
        if (labels == null || labels.isEmpty()) {
            return "mappingFailures=" + safe;
        }
        return "mappingFailures=" + safe + " [" + String.join(", ", labels) + "]";
    }

    public static String teamsLabel(String home, String away) {
        String h = home != null ? home.trim() : "";
        String a = away != null ? away.trim() : "";
        if (h.isEmpty() && a.isEmpty()) {
            return null;
        }
        if (h.isEmpty()) {
            return a;
        }
        if (a.isEmpty()) {
            return h;
        }
        return h + " - " + a;
    }

    private final ExternalApiMonitoringRepository repository;
    private final ErrorLogService errorLogService;
    private final MatchScheduleRepository matchScheduleRepository;
    private final TeamsRepository teamsRepository;

    public static void setTriggerOverride(ExternalApiMonitoringTrigger trigger) {
        TRIGGER_OVERRIDE.set(trigger);
    }

    public static void clearTriggerOverride() {
        TRIGGER_OVERRIDE.remove();
    }

    public static ExternalApiMonitoringTrigger effectiveTrigger(ExternalApiMonitoringTrigger fallback) {
        ExternalApiMonitoringTrigger override = TRIGGER_OVERRIDE.get();
        return override != null ? override : fallback;
    }

    public ExternalApiMonitoringRun begin(
            ExternalDataLayer layer,
            String provider,
            ExternalApiMonitoringTrigger trigger,
            String leagueCode,
            String season
    ) {
        ExternalApiMonitoringTrigger resolved = effectiveTrigger(trigger);
        return ExternalApiMonitoringRun.builder()
                .layer(layer)
                .provider(provider)
                .trigger(resolved)
                .manual(resolved == ExternalApiMonitoringTrigger.ADMIN)
                .leagueCode(leagueCode)
                .season(season)
                .startedAt(Instant.now())
                .status(ExternalApiMonitoringStatus.SUCCESS)
                .counters(new ExternalApiMonitoringCounters())
                .httpLogs(new ArrayList<>())
                .failedMatchScheduleIds(new ArrayList<>())
                .failedMatchLabels(new ArrayList<>())
                .failedMatches(new ArrayList<>())
                .build();
    }

    public ExternalApiMonitoringRun finalizeAndSave(
            ExternalApiMonitoringRun run,
            ExternalApiMonitoringStatus status,
            ExternalApiMonitoringCounters counters,
            List<ExternalApiHttpLogEntry> httpLogs,
            List<String> failedMatchScheduleIds,
            String errorSummary
    ) {
        if (run == null) {
            throw new BadRequestException("externalApiMonitoringRunRequired");
        }
        run.setStatus(status != null ? status : ExternalApiMonitoringStatus.SUCCESS);
        run.setCounters(counters != null ? counters : new ExternalApiMonitoringCounters());
        List<ExternalApiHttpLogEntry> logs = httpLogs != null ? httpLogs : List.of();
        run.setHttpLogs(new ArrayList<>(logs));
        run.setHttpRequestsTotal(logs.size());
        run.setHttpRequestsFailed(countFailed(logs));

        List<String> failedIds = List.of();
        List<ExternalApiMatchTeamsRef> failedMatches = List.of();
        List<String> failedLabels = List.of();
        if (failedMatchScheduleIds != null && !failedMatchScheduleIds.isEmpty()) {
            failedIds = new ArrayList<>(new LinkedHashSet<>(failedMatchScheduleIds));
            failedMatches = resolveMatchTeams(failedIds);
            failedLabels = failedMatches.stream()
                    .map(ExternalApiMatchTeamsRef::label)
                    .filter(l -> l != null && !l.isBlank())
                    .toList();
            run.setFailedMatchScheduleIds(new ArrayList<>(failedIds));
            run.setFailedMatchLabels(new ArrayList<>(failedLabels));
            run.setFailedMatches(new ArrayList<>(failedMatches));
        }

        String summary = enrichMappingFailuresSummary(errorSummary, failedLabels);
        run.setErrorSummary(summary);

        if (run.getHttpRequestsFailed() > 0) {
            errorLogService.recordHttpRequestFailuresIfNeeded(
                    run.getLayer(),
                    run.getProvider(),
                    run.getLeagueCode(),
                    run.getSeason(),
                    logs,
                    summary,
                    failedIds
            );
        } else if (!failedIds.isEmpty() && summary != null && summary.contains("mappingFailures=")) {
            errorLogService.recordProviderMessageIfNeeded(
                    run.getLayer(),
                    run.getProvider(),
                    run.getLeagueCode(),
                    run.getSeason(),
                    summary,
                    failedIds
            );
        }

        Instant finishedAt = Instant.now();
        run.setFinishedAt(finishedAt);
        if (run.getStartedAt() != null) {
            run.setDurationMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        }
        return repository.save(run);
    }

    public List<String> resolveMatchLabels(List<String> matchScheduleIds) {
        return resolveMatchTeams(matchScheduleIds).stream()
                .map(ExternalApiMatchTeamsRef::label)
                .filter(l -> l != null && !l.isBlank())
                .toList();
    }

    public List<ExternalApiMatchTeamsRef> resolveMatchTeams(List<String> matchScheduleIds) {
        if (matchScheduleIds == null || matchScheduleIds.isEmpty()) {
            return List.of();
        }
        Map<String, MatchSchedule> schedules = new HashMap<>();
        for (MatchSchedule schedule : matchScheduleRepository.findAllById(matchScheduleIds)) {
            if (schedule.getId() != null) {
                schedules.put(schedule.getId(), schedule);
            }
        }
        if (schedules.isEmpty()) {
            return List.of();
        }
        Set<String> teamIds = new HashSet<>();
        for (MatchSchedule schedule : schedules.values()) {
            if (schedule.getHomeTeamId() != null && !schedule.getHomeTeamId().isBlank()) {
                teamIds.add(schedule.getHomeTeamId());
            }
            if (schedule.getAwayTeamId() != null && !schedule.getAwayTeamId().isBlank()) {
                teamIds.add(schedule.getAwayTeamId());
            }
        }
        Map<String, Team> teams = new HashMap<>();
        if (!teamIds.isEmpty()) {
            for (Team team : teamsRepository.findAllById(teamIds)) {
                if (team.getId() != null) {
                    teams.put(team.getId(), team);
                }
            }
        }
        List<ExternalApiMatchTeamsRef> refs = new ArrayList<>();
        for (String id : matchScheduleIds) {
            MatchSchedule schedule = schedules.get(id);
            if (schedule == null) {
                continue;
            }
            Team home = teams.get(schedule.getHomeTeamId());
            Team away = teams.get(schedule.getAwayTeamId());
            refs.add(ExternalApiMatchTeamsRef.builder()
                    .matchScheduleId(id)
                    .homeTitle(teamTitle(home, schedule.getHomeTeamId()))
                    .awayTitle(teamTitle(away, schedule.getAwayTeamId()))
                    .homeLogoKey(logoKey(home))
                    .awayLogoKey(logoKey(away))
                    .build());
        }
        return refs;
    }

    public ExternalApiMatchTeamsRef resolveMatchTeams(MatchSchedule match) {
        if (match == null || match.getId() == null) {
            return null;
        }
        List<ExternalApiMatchTeamsRef> refs = resolveMatchTeams(List.of(match.getId()));
        return refs.isEmpty() ? null : refs.get(0);
    }

    private static String teamTitle(Team team, String fallbackId) {
        if (team != null && team.getTitle() != null && !team.getTitle().isBlank()) {
            return team.getTitle().trim();
        }
        return fallbackId;
    }

    public static String logoKey(Team team) {
        if (team == null) {
            return null;
        }
        if (team.getLogo() != null && !team.getLogo().isBlank()) {
            return team.getLogo().trim();
        }
        if (team.getTitle() != null && !team.getTitle().isBlank()) {
            return team.getTitle().trim();
        }
        return null;
    }

    /**
     * If summary already has {@code mappingFailures=N} without bracket labels, append resolved labels.
     */
    static String enrichMappingFailuresSummary(String errorSummary, List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return errorSummary;
        }
        if (errorSummary == null || errorSummary.isBlank()) {
            return mappingFailuresSummary(labels.size(), labels);
        }
        String[] parts = errorSummary.split(";");
        StringBuilder out = new StringBuilder();
        boolean replaced = false;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append("; ");
            }
            if (!replaced && trimmed.startsWith("mappingFailures=")) {
                int eq = trimmed.indexOf('=');
                String afterEq = eq >= 0 ? trimmed.substring(eq + 1).trim() : "";
                int count;
                try {
                    int bracket = afterEq.indexOf('[');
                    String countPart = bracket >= 0 ? afterEq.substring(0, bracket).trim() : afterEq;
                    count = Integer.parseInt(countPart);
                } catch (NumberFormatException e) {
                    count = labels.size();
                }
                if (!afterEq.contains("[")) {
                    out.append(mappingFailuresSummary(count, labels));
                    replaced = true;
                    continue;
                }
            }
            out.append(trimmed);
        }
        if (!replaced) {
            // Failed IDs without a mappingFailures= token: keep original summary as-is.
            return errorSummary.trim();
        }
        return out.toString();
    }

    public List<ExternalApiMonitoringRun> listByLayer(
            ExternalDataLayer layer,
            int hours,
            int limit,
            int offset,
            List<ExternalApiMonitoringStatus> statusFilter
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int safeOffset = Math.max(0, offset);
        int safeHours = Math.max(1, Math.min(hours, 24 * 30));
        Instant after = Instant.now().minus(Duration.ofHours(safeHours));
        int page = safeOffset / safeLimit;
        PageRequest pageable = PageRequest.of(page, safeLimit);
        if (statusFilter != null && !statusFilter.isEmpty()) {
            return repository.findByLayerAndStatusInAndStartedAtAfterOrderByStartedAtDesc(
                    layer,
                    statusFilter,
                    after,
                    pageable
            );
        }
        return repository.findByLayerAndStartedAtAfterOrderByStartedAtDesc(
                layer,
                after,
                pageable
        );
    }

    public ExternalApiMonitoringLayerPageDto listPageByLayer(
            ExternalDataLayer layer,
            int hours,
            int limit,
            int offset,
            List<ExternalApiMonitoringStatus> statusFilter
    ) {
        int safeHours = Math.max(1, Math.min(hours, 24 * 30));
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int safeOffset = Math.max(0, offset);
        Instant after = Instant.now().minus(Duration.ofHours(safeHours));
        List<ExternalApiMonitoringRunDto> runs = listByLayer(layer, hours, limit, offset, statusFilter).stream()
                .map(ExternalApiMonitoringRunDto::summary)
                .toList();
        long total = statusFilter != null && !statusFilter.isEmpty()
                ? repository.countByLayerAndStatusInAndStartedAtAfter(layer, statusFilter, after)
                : repository.countByLayerAndStartedAtAfter(layer, after);
        long failed = repository.countByLayerAndStatusAndStartedAtAfter(
                layer,
                ExternalApiMonitoringStatus.FAILED,
                after
        );
        boolean hasMore = (long) safeOffset + runs.size() < total;
        return ExternalApiMonitoringLayerPageDto.builder()
                .runs(runs)
                .total(total)
                .failed(failed)
                .offset(safeOffset)
                .limit(safeLimit)
                .hasMore(hasMore)
                .build();
    }

    public static List<ExternalApiMonitoringStatus> parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<ExternalApiMonitoringStatus> out = new ArrayList<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if ("issues".equalsIgnoreCase(trimmed)) {
                out.add(ExternalApiMonitoringStatus.FAILED);
                out.add(ExternalApiMonitoringStatus.PARTIAL);
                continue;
            }
            try {
                out.add(ExternalApiMonitoringStatus.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                throw new BadRequestException("externalApiMonitoringStatusInvalid");
            }
        }
        return out.stream().distinct().toList();
    }

    public ExternalApiMonitoringRun getById(String id) {
        return enrichFailedLabelsIfMissing(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("ExternalApiMonitoringRun", id)));
    }

    public ExternalApiMonitoringRun latestByLayer(ExternalDataLayer layer) {
        return repository.findFirstByLayerOrderByStartedAtDesc(layer);
    }

    private ExternalApiMonitoringRun enrichFailedLabelsIfMissing(ExternalApiMonitoringRun run) {
        if (run == null) {
            return null;
        }
        List<String> ids = run.getFailedMatchScheduleIds();
        if (ids == null || ids.isEmpty()) {
            return run;
        }
        boolean hasLabels = run.getFailedMatchLabels() != null && !run.getFailedMatchLabels().isEmpty();
        boolean hasMatches = run.getFailedMatches() != null && !run.getFailedMatches().isEmpty();
        if (hasLabels && hasMatches) {
            return run;
        }
        List<ExternalApiMatchTeamsRef> refs = resolveMatchTeams(ids);
        if (!hasMatches) {
            run.setFailedMatches(new ArrayList<>(refs));
        }
        if (!hasLabels) {
            run.setFailedMatchLabels(refs.stream()
                    .map(ExternalApiMatchTeamsRef::label)
                    .filter(l -> l != null && !l.isBlank())
                    .toList());
        }
        return run;
    }

    @Transactional
    public long deleteByLayer(ExternalDataLayer layer) {
        if (layer == null) {
            throw new BadRequestException("externalApiMonitoringLayerRequired");
        }
        return repository.deleteByLayer(layer);
    }

    public static int countFailed(List<ExternalApiHttpLogEntry> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.stream()
                .filter(e -> e.getOutcome() != null && !"SUCCESS".equals(e.getOutcome()))
                .count();
    }

    public static ExternalApiHttpLogEntry httpLog(
            String requestType,
            String target,
            Integer httpStatus,
            String outcome,
            long durationMs,
            String detail,
            Integer retryAfterSeconds,
            Instant requestedAt
    ) {
        return httpLog(requestType, target, (ExternalApiMatchTeamsRef) null, httpStatus, outcome, durationMs, detail, retryAfterSeconds, requestedAt);
    }

    public static ExternalApiHttpLogEntry httpLog(
            String requestType,
            String target,
            String teams,
            Integer httpStatus,
            String outcome,
            long durationMs,
            String detail,
            Integer retryAfterSeconds,
            Instant requestedAt
    ) {
        return httpLog(requestType, target, teams, null, null, null, null, httpStatus, outcome, durationMs, detail, retryAfterSeconds, requestedAt);
    }

    public static ExternalApiHttpLogEntry httpLog(
            String requestType,
            String target,
            ExternalApiMatchTeamsRef teamsRef,
            Integer httpStatus,
            String outcome,
            long durationMs,
            String detail,
            Integer retryAfterSeconds,
            Instant requestedAt
    ) {
        String teams = teamsRef != null ? teamsRef.label() : null;
        return httpLog(
                requestType,
                target,
                teams,
                teamsRef != null ? teamsRef.getHomeTitle() : null,
                teamsRef != null ? teamsRef.getAwayTitle() : null,
                teamsRef != null ? teamsRef.getHomeLogoKey() : null,
                teamsRef != null ? teamsRef.getAwayLogoKey() : null,
                httpStatus,
                outcome,
                durationMs,
                detail,
                retryAfterSeconds,
                requestedAt
        );
    }

    public static ExternalApiHttpLogEntry httpLog(
            String requestType,
            String target,
            String teams,
            String homeTitle,
            String awayTitle,
            String homeLogoKey,
            String awayLogoKey,
            Integer httpStatus,
            String outcome,
            long durationMs,
            String detail,
            Integer retryAfterSeconds,
            Instant requestedAt
    ) {
        return ExternalApiHttpLogEntry.builder()
                .requestType(requestType)
                .target(target)
                .teams(teams)
                .homeTitle(homeTitle)
                .awayTitle(awayTitle)
                .homeLogoKey(homeLogoKey)
                .awayLogoKey(awayLogoKey)
                .httpStatus(httpStatus)
                .outcome(outcome)
                .durationMs(durationMs)
                .detail(detail)
                .retryAfterSeconds(retryAfterSeconds)
                .requestedAt(requestedAt != null ? requestedAt : Instant.now())
                .build();
    }
}
