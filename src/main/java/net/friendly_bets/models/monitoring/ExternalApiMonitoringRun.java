package net.friendly_bets.models.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "external_api_monitoring")
@CompoundIndex(name = "layer_started_at", def = "{'layer': 1, 'started_at': -1}")
public class ExternalApiMonitoringRun {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "layer")
    @Indexed
    private ExternalDataLayer layer;

    @Field(name = "provider")
    private String provider;

    @Field(name = "trigger")
    private ExternalApiMonitoringTrigger trigger;

    @Field(name = "status")
    private ExternalApiMonitoringStatus status;

    @Field(name = "started_at")
    @Indexed
    private Instant startedAt;

    @Field(name = "finished_at")
    private Instant finishedAt;

    @Field(name = "duration_ms")
    private Long durationMs;

    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "season")
    private String season;

    @Field(name = "matchday")
    private Integer matchday;

    @Field(name = "slot_orders")
    @Builder.Default
    private List<Integer> slotOrders = new ArrayList<>();

    @Field(name = "slot_scope")
    private String slotScope;

    @Field(name = "manual")
    private boolean manual;

    @Field(name = "counters")
    @Builder.Default
    private ExternalApiMonitoringCounters counters = new ExternalApiMonitoringCounters();

    @Field(name = "http_requests_total")
    private int httpRequestsTotal;

    @Field(name = "http_requests_failed")
    private int httpRequestsFailed;

    @Field(name = "http_logs")
    @Builder.Default
    private List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();

    @Field(name = "error_summary")
    private String errorSummary;

    @Field(name = "failed_match_schedule_ids")
    @Builder.Default
    private List<String> failedMatchScheduleIds = new ArrayList<>();

    @Field(name = "failover_used")
    private boolean failoverUsed;
}
