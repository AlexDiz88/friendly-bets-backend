package net.friendly_bets.gameresults;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AutoSettleResult {
    String seasonId;
    int matchesSubmitted;
    int betsProcessed;
    boolean executed;
}
