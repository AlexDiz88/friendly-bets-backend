package net.friendly_bets.marathonbet;

import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetHttpOutcome;
import net.friendly_bets.marathonbet.client.MarathonbetRequestType;
import net.friendly_bets.models.marathonbet.MarathonbetHttpLogEntry;

import java.time.LocalDateTime;
import java.util.List;

public final class MarathonbetHttpLogSupport {

    private MarathonbetHttpLogSupport() {
    }

    public static MarathonbetHttpLogEntry toLogEntry(
            MarathonbetHttpFetchResult result,
            MarathonbetRequestType type,
            long targetId,
            LocalDateTime requestedAt
    ) {
        return MarathonbetHttpLogEntry.builder()
                .requestType(type.name())
                .targetId(targetId)
                .httpStatus(result.getHttpStatus())
                .outcome(result.getOutcome().name())
                .durationMs(result.getDurationMs())
                .detail(result.getErrorDetail())
                .retryAfterSeconds(result.getRetryAfterSeconds())
                .requestedAt(requestedAt)
                .build();
    }

    public static int countFailed(List<MarathonbetHttpLogEntry> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.stream()
                .filter(e -> e.getOutcome() != null
                        && !MarathonbetHttpOutcome.SUCCESS.name().equals(e.getOutcome()))
                .count();
    }
}
