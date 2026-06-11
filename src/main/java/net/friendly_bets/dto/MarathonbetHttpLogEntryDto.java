package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class MarathonbetHttpLogEntryDto {
    String requestType;
    Long targetId;
    Integer httpStatus;
    String outcome;
    long durationMs;
    String detail;
    Integer retryAfterSeconds;
    LocalDateTime requestedAt;
}
