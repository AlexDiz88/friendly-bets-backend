package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.providers.ExternalDataLayer;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ExternalApiMonitoringRunDto {
    String id;
    ExternalDataLayer layer;
    String provider;
    ExternalApiMonitoringTrigger trigger;
    ExternalApiMonitoringStatus status;
    Instant startedAt;
    Instant finishedAt;
    Long durationMs;
    String leagueCode;
    String season;
    Integer matchday;
    List<Integer> slotOrders;
    String slotScope;
    boolean manual;
    ExternalApiMonitoringCounters counters;
    int httpRequestsTotal;
    int httpRequestsFailed;
    List<ExternalApiHttpLogEntry> httpLogs;
    String errorSummary;
    List<String> failedMatchScheduleIds;
    boolean failoverUsed;

    public static ExternalApiMonitoringRunDto from(ExternalApiMonitoringRun run) {
        return ExternalApiMonitoringRunDto.builder()
                .id(run.getId())
                .layer(run.getLayer())
                .provider(run.getProvider())
                .trigger(run.getTrigger())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .durationMs(run.getDurationMs())
                .leagueCode(run.getLeagueCode())
                .season(run.getSeason())
                .matchday(run.getMatchday())
                .slotOrders(run.getSlotOrders())
                .slotScope(run.getSlotScope())
                .manual(run.isManual())
                .counters(run.getCounters())
                .httpRequestsTotal(run.getHttpRequestsTotal())
                .httpRequestsFailed(run.getHttpRequestsFailed())
                .httpLogs(run.getHttpLogs())
                .errorSummary(run.getErrorSummary())
                .failedMatchScheduleIds(run.getFailedMatchScheduleIds())
                .failoverUsed(run.isFailoverUsed())
                .build();
    }

    /** List view without full http_logs payload. */
    public static ExternalApiMonitoringRunDto summary(ExternalApiMonitoringRun run) {
        return ExternalApiMonitoringRunDto.builder()
                .id(run.getId())
                .layer(run.getLayer())
                .provider(run.getProvider())
                .trigger(run.getTrigger())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .durationMs(run.getDurationMs())
                .leagueCode(run.getLeagueCode())
                .season(run.getSeason())
                .matchday(run.getMatchday())
                .slotOrders(run.getSlotOrders())
                .slotScope(run.getSlotScope())
                .manual(run.isManual())
                .counters(run.getCounters())
                .httpRequestsTotal(run.getHttpRequestsTotal())
                .httpRequestsFailed(run.getHttpRequestsFailed())
                .httpLogs(List.of())
                .errorSummary(run.getErrorSummary())
                .failedMatchScheduleIds(run.getFailedMatchScheduleIds())
                .failoverUsed(run.isFailoverUsed())
                .build();
    }
}
