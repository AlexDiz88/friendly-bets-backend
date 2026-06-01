package net.friendly_bets.oddsapi.mapping;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.List;

@Value
@Builder
public class OddsMergeResult {

    List<OddsMarketGroup> marketGroups;
    List<MappedOddsQuote> allQuotes;
    List<MappedOddsQuote> rejectedQuotes;
    List<OddsCrossBookmakerMismatch> mismatches;
}
