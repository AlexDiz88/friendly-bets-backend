package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarathonbetRequestStatsDto {
    int periodHours;
    int totalRuns;
    int manualRuns;
    int scheduledRuns;
    int tournamentRequests;
    int sseRequests;
    int httpFailures;
    int rateLimitedCount;
    int accessDeniedCount;
    int timeoutCount;
    long avgSseDurationMs;
    int matchesSaved;
    int mappingFailures;
}
