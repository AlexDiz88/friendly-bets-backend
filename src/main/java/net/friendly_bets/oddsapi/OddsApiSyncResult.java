package net.friendly_bets.oddsapi;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OddsApiSyncResult {

    int leaguesProcessed;
    int matchesEligible;
    int oddsDocumentsSaved;
    int matchesSkippedStarted;
    int mappingFailures;
}
