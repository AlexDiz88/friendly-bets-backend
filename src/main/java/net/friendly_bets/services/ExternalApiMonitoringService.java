package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ExternalApiMonitoringService {

    private static final ThreadLocal<ExternalApiMonitoringTrigger> TRIGGER_OVERRIDE = new ThreadLocal<>();

    private final ExternalApiMonitoringRepository repository;

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

    public List<ExternalApiMonitoringRun> listByLayer(ExternalDataLayer layer, int hours, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeHours = Math.max(1, Math.min(hours, 24 * 30));
        Instant after = Instant.now().minus(Duration.ofHours(safeHours));
        return repository.findByLayerAndStartedAtAfterOrderByStartedAtDesc(
                layer,
                after,
                PageRequest.of(0, safeLimit)
        );
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
