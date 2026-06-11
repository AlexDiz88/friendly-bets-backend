package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.MarathonbetRequestStatsDto;
import net.friendly_bets.marathonbet.client.MarathonbetHttpOutcome;
import net.friendly_bets.marathonbet.client.MarathonbetRequestType;
import net.friendly_bets.models.marathonbet.MarathonbetHttpLogEntry;
import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import net.friendly_bets.repositories.MarathonbetSyncRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarathonbetRequestStatsService {

    private final MarathonbetSyncRunRepository syncRunRepository;

    public MarathonbetRequestStatsDto buildStats(int periodHours) {
        int hours = Math.max(1, Math.min(periodHours, 24 * 30));
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<MarathonbetSyncRun> runs = syncRunRepository.findByStartedAtAfterOrderByStartedAtDesc(
                since,
                PageRequest.of(0, 500)
        );

        int manualRuns = 0;
        int tournamentRequests = 0;
        int sseRequests = 0;
        int httpFailures = 0;
        int rateLimitedCount = 0;
        int accessDeniedCount = 0;
        int timeoutCount = 0;
        long sseDurationSum = 0;
        int matchesSaved = 0;
        int mappingFailures = 0;

        for (MarathonbetSyncRun run : runs) {
            if (run.isManual()) {
                manualRuns++;
            }
            matchesSaved += run.getMergedSaved();
            mappingFailures += run.getMappingFailures();
            if (run.getHttpLogs() == null) {
                continue;
            }
            for (MarathonbetHttpLogEntry entry : run.getHttpLogs()) {
                if (MarathonbetRequestType.TOURNAMENT.name().equals(entry.getRequestType())) {
                    tournamentRequests++;
                } else if (MarathonbetRequestType.SSE.name().equals(entry.getRequestType())) {
                    sseRequests++;
                    if (MarathonbetHttpOutcome.SUCCESS.name().equals(entry.getOutcome())) {
                        sseDurationSum += entry.getDurationMs();
                    }
                }
                if (!MarathonbetHttpOutcome.SUCCESS.name().equals(entry.getOutcome())) {
                    httpFailures++;
                }
                if (entry.getHttpStatus() != null && entry.getHttpStatus() == 429) {
                    rateLimitedCount++;
                }
                if (entry.getHttpStatus() != null && entry.getHttpStatus() == 403) {
                    accessDeniedCount++;
                }
                if (MarathonbetHttpOutcome.TIMEOUT.name().equals(entry.getOutcome())) {
                    timeoutCount++;
                }
            }
        }

        int successfulSse = sseRequests - countFailedSse(runs);
        long avgSseDurationMs = successfulSse > 0 ? sseDurationSum / successfulSse : 0;

        return MarathonbetRequestStatsDto.builder()
                .periodHours(hours)
                .totalRuns(runs.size())
                .manualRuns(manualRuns)
                .scheduledRuns(runs.size() - manualRuns)
                .tournamentRequests(tournamentRequests)
                .sseRequests(sseRequests)
                .httpFailures(httpFailures)
                .rateLimitedCount(rateLimitedCount)
                .accessDeniedCount(accessDeniedCount)
                .timeoutCount(timeoutCount)
                .avgSseDurationMs(avgSseDurationMs)
                .matchesSaved(matchesSaved)
                .mappingFailures(mappingFailures)
                .build();
    }

    private static int countFailedSse(List<MarathonbetSyncRun> runs) {
        int failed = 0;
        for (MarathonbetSyncRun run : runs) {
            if (run.getHttpLogs() == null) {
                continue;
            }
            for (MarathonbetHttpLogEntry entry : run.getHttpLogs()) {
                if (MarathonbetRequestType.SSE.name().equals(entry.getRequestType())
                        && !MarathonbetHttpOutcome.SUCCESS.name().equals(entry.getOutcome())) {
                    failed++;
                }
            }
        }
        return failed;
    }
}
