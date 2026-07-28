package net.friendly_bets.melbet;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class MelbetSyncResult {
    @Builder.Default
    boolean tournamentFetched = false;
    @Builder.Default
    int matchesEligible = 0;
    @Builder.Default
    int matchesMatched = 0;
    @Builder.Default
    int mergedSaved = 0;
    @Builder.Default
    int eventCalls = 0;
    @Builder.Default
    int mappingFailures = 0;
    @Builder.Default
    int skippedFar = 0;
    @Builder.Default
    int skippedNoBookieEvent = 0;
    @Builder.Default
    List<String> failedMatchScheduleIds = new ArrayList<>();
    String leagueCode;
    String season;
    @Builder.Default
    List<Integer> slotOrders = new ArrayList<>();
    String errorSummary;
}
