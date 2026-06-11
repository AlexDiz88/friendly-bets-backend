package net.friendly_bets.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class MarathonbetSyncRunDto {
    String id;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    Long durationMs;
    String leagueCode;
    String season;
    String slotScope;
    List<Integer> slotOrders;
    boolean tournamentFetched;
    int matchesEligible;
    int matchesMatched;
    int mergedSaved;
    int sseCalls;
    int mappingFailures;
    int httpRequestsTotal;
    int httpRequestsFailed;
    boolean fallbackUsed;
    boolean manual;
    String errorSummary;
    List<MarathonbetHttpLogEntryDto> httpLogs;
}
