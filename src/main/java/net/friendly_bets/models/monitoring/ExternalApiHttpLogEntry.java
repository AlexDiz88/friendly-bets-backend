package net.friendly_bets.models.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExternalApiHttpLogEntry {

    @Field(name = "request_type")
    private String requestType;

    /** Provider-specific target (URL, treeId, gameId, …). */
    @Field(name = "target")
    private String target;

    /** Optional "Home - Away" label for match-scoped requests (SSE / EVENT). */
    @Field(name = "teams")
    private String teams;

    @Field(name = "home_title")
    private String homeTitle;

    @Field(name = "away_title")
    private String awayTitle;

    @Field(name = "home_logo_key")
    private String homeLogoKey;

    @Field(name = "away_logo_key")
    private String awayLogoKey;

    @Field(name = "http_status")
    private Integer httpStatus;

    @Field(name = "outcome")
    private String outcome;

    @Field(name = "duration_ms")
    private long durationMs;

    @Field(name = "detail")
    private String detail;

    @Field(name = "retry_after_seconds")
    private Integer retryAfterSeconds;

    @Field(name = "requested_at")
    private Instant requestedAt;
}
