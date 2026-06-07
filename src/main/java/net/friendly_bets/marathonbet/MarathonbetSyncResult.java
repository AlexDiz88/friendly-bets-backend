package net.friendly_bets.marathonbet;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class MarathonbetSyncResult {
    @Builder.Default
    boolean tournamentFetched = false;
    @Builder.Default
    int matchesEligible = 0;
    @Builder.Default
    int matchesMatched = 0;
    @Builder.Default
    int mergedSaved = 0;
    @Builder.Default
    int sseCalls = 0;
    @Builder.Default
    int mappingFailures = 0;
    @Builder.Default
    List<String> failedGameResultIds = new ArrayList<>();
    String leagueCode;
    String season;
    @Builder.Default
    List<Integer> slotOrders = new ArrayList<>();
    String errorSummary;

    public double matchRatio() {
        if (matchesEligible <= 0) {
            return 0;
        }
        return (double) matchesMatched / matchesEligible;
    }
}
