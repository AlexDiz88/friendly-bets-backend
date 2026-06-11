package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.MarathonbetHttpLogEntryDto;
import net.friendly_bets.dto.MarathonbetSyncRunDto;
import net.friendly_bets.models.marathonbet.MarathonbetHttpLogEntry;
import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;

import java.util.List;

public final class MarathonbetSyncRunMapper {

    private MarathonbetSyncRunMapper() {
    }

    public static MarathonbetSyncRunDto toDto(MarathonbetSyncRun run) {
        return MarathonbetSyncRunDto.builder()
                .id(run.getId())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .durationMs(run.getDurationMs())
                .leagueCode(run.getLeagueCode())
                .season(run.getSeason())
                .slotScope(run.getSlotScope())
                .slotOrders(run.getSlotOrders())
                .tournamentFetched(run.isTournamentFetched())
                .matchesEligible(run.getMatchesEligible())
                .matchesMatched(run.getMatchesMatched())
                .mergedSaved(run.getMergedSaved())
                .sseCalls(run.getSseCalls())
                .mappingFailures(run.getMappingFailures())
                .httpRequestsTotal(run.getHttpRequestsTotal())
                .httpRequestsFailed(run.getHttpRequestsFailed())
                .fallbackUsed(run.isFallbackUsed())
                .manual(run.isManual())
                .errorSummary(run.getErrorSummary())
                .httpLogs(toLogDtos(run.getHttpLogs()))
                .build();
    }

    private static List<MarathonbetHttpLogEntryDto> toLogDtos(List<MarathonbetHttpLogEntry> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream().map(MarathonbetSyncRunMapper::toLogDto).toList();
    }

    private static MarathonbetHttpLogEntryDto toLogDto(MarathonbetHttpLogEntry entry) {
        return MarathonbetHttpLogEntryDto.builder()
                .requestType(entry.getRequestType())
                .targetId(entry.getTargetId())
                .httpStatus(entry.getHttpStatus())
                .outcome(entry.getOutcome())
                .durationMs(entry.getDurationMs())
                .detail(entry.getDetail())
                .retryAfterSeconds(entry.getRetryAfterSeconds())
                .requestedAt(entry.getRequestedAt())
                .build();
    }
}
