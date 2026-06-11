package net.friendly_bets.models.marathonbet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MarathonbetHttpLogEntry {

    @Field(name = "request_type")
    private String requestType;

    @Field(name = "target_id")
    private Long targetId;

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
    private LocalDateTime requestedAt;
}
