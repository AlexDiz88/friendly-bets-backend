package net.friendly_bets.models.marathonbet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "marathonbet_sync_runs")
public class MarathonbetSyncRun {

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "started_at")
    @Indexed
    private LocalDateTime startedAt;

    @Field(name = "finished_at")
    private LocalDateTime finishedAt;

    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "season")
    private String season;

    @Field(name = "slot_orders")
    @Builder.Default
    private List<Integer> slotOrders = new ArrayList<>();

    @Field(name = "tournament_fetched")
    private boolean tournamentFetched;

    @Field(name = "matches_eligible")
    private int matchesEligible;

    @Field(name = "matches_matched")
    private int matchesMatched;

    @Field(name = "merged_saved")
    private int mergedSaved;

    @Field(name = "sse_calls")
    private int sseCalls;

    @Field(name = "mapping_failures")
    private int mappingFailures;

    @Field(name = "fallback_used")
    private boolean fallbackUsed;

    @Field(name = "manual")
    private boolean manual;

    @Field(name = "slot_scope")
    private String slotScope;

    @Field(name = "duration_ms")
    private Long durationMs;

    @Field(name = "http_requests_total")
    private int httpRequestsTotal;

    @Field(name = "http_requests_failed")
    private int httpRequestsFailed;

    @Field(name = "http_logs")
    @Builder.Default
    private List<MarathonbetHttpLogEntry> httpLogs = new ArrayList<>();

    @Field(name = "error_summary")
    private String errorSummary;

    @Field(name = "failed_game_result_ids")
    @Builder.Default
    private List<String> failedGameResultIds = new ArrayList<>();
}
