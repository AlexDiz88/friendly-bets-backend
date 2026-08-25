package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "error_logs")
@CompoundIndexes({
        @CompoundIndex(name = "created_at_desc", def = "{'created_at': -1}"),
        @CompoundIndex(name = "layer_created", def = "{'layer': 1, 'created_at': -1}"),
        @CompoundIndex(name = "provider_code_match", def = "{'provider': 1, 'code': 1, 'match_schedule_id': 1}")
})
public class ErrorLog {

    @MongoId
    @Field(name = "_id")
    private String id;

    /** First occurrence. Never bumped on repeats — last time lives in {@code occurredAt}. */
    @Field(name = "created_at")
    @Indexed
    private Instant createdAt;

    /** ERROR or WARN */
    @Field(name = "severity")
    private String severity;

    /** SCHEDULE / ODDS / LIVE / FULL_MATCH — optional for non-layer errors */
    @Field(name = "layer")
    private String layer;

    @Field(name = "provider")
    private String provider;

    /** PRIMARY / SECONDARY */
    @Field(name = "provider_role")
    private String providerRole;

    /** Stable machine code, e.g. teamMappingMissing, soccer365FetchFailed */
    @Field(name = "code")
    private String code;

    @Field(name = "message")
    private String message;

    @Field(name = "league_code")
    private String leagueCode;

    @Field(name = "season")
    private String season;

    @Field(name = "matchday")
    private Integer matchday;

    @Field(name = "match_schedule_id")
    private String matchScheduleId;

    @Field(name = "external_match_id")
    private String externalMatchId;

    @Field(name = "home_team")
    private String homeTeam;

    @Field(name = "away_team")
    private String awayTeam;

    /** Same as {@code created_at} / first of {@code occurredAt}; kept for older documents. */
    @Field(name = "first_occurred_at")
    private Instant firstOccurredAt;

    /** Last occurrence; used to keep the row among recent logs without rewriting {@code created_at}. */
    @Field(name = "last_occurred_at")
    @Indexed
    private Instant lastOccurredAt;

    /** Every failure Instant for this incident (same provider+code+match). */
    @Field(name = "occurred_at")
    @Builder.Default
    private List<Instant> occurredAt = new ArrayList<>();

    /** Times this row was recorded; equals {@code occurredAt.size()} when the array is present. */
    @Field(name = "occurrence_count")
    private Integer occurrenceCount;

    /** Extra chips: homeExternalId, awayExternalId, … */
    @Field(name = "context")
    @Builder.Default
    private Map<String, String> context = new LinkedHashMap<>();
}
