package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalApiMonitoringLayerPageDto;
import net.friendly_bets.dto.ExternalApiMonitoringRunDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.ExternalApiMonitoringRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
        return ODDS_CRON_SOFT_SKIP_REASONS.contains(key);
    }

    private final ExternalApiMonitoringRepository repository;
    private final ErrorLogService errorLogService;

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
        if (run.getHttpRequestsFailed() > 0) {
            errorLogService.recordHttpRequestFailuresIfNeeded(
                    run.getLayer(),
                    run.getProvider(),
                    run.getLeagueCode(),
                    run.getSeason(),
                    logs,
                    errorSummary
            );
        }
        if (failedMatchScheduleIds != null && !failedMatchScheduleIds.isEmpty()) {
            run.setFailedMatchScheduleIds(new ArrayList<>(new LinkedHashSet<>(failedMatchScheduleIds)));
        }
        run.setErrorSummary(errorSummary);
        Instant finishedAt = Instant.now();
        run.setFinishedAt(finishedAt);
        if (run.getStartedAt() != null) {
            run.setDurationMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        }
        return repository.save(run);
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
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("ExternalApiMonitoringRun", id));
    }

    public ExternalApiMonitoringRun latestByLayer(ExternalDataLayer layer) {
        return repository.findFirstByLayerOrderByStartedAtDesc(layer);
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
        return ExternalApiHttpLogEntry.builder()
                .requestType(requestType)
                .target(target)
                .httpStatus(httpStatus)
                .outcome(outcome)
                .durationMs(durationMs)
                .detail(detail)
                .retryAfterSeconds(retryAfterSeconds)
                .requestedAt(requestedAt != null ? requestedAt : Instant.now())
                .build();
    }
}
