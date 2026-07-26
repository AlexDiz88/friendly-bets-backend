package net.friendly_bets.marathonbet;

import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetRequestType;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.services.ExternalApiMonitoringService;

import java.time.LocalDateTime;

public final class MarathonbetHttpLogSupport {

    private MarathonbetHttpLogSupport() {
    }

    public static ExternalApiHttpLogEntry toLogEntry(
            MarathonbetHttpFetchResult result,
            MarathonbetRequestType type,
            long targetId,
            LocalDateTime requestedAt
    ) {
        return ExternalApiMonitoringService.httpLog(
                type.name(),
                String.valueOf(targetId),
                result.getHttpStatus(),
                result.getOutcome() != null ? result.getOutcome().name() : null,
                result.getDurationMs(),
                result.getErrorDetail(),
                result.getRetryAfterSeconds(),
                requestedAt
        );
    }
}
