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
    String leagueCode;
    String season;
    List<Integer> slotOrders;
    boolean tournamentFetched;
    int matchesEligible;
    int matchesMatched;
    int mergedSaved;
    int sseCalls;
    int mappingFailures;
    boolean fallbackUsed;
    boolean manual;
    String errorSummary;
}
