package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import net.friendly_bets.oddsapi.mapping.Bet365OddsAdapter;
import net.friendly_bets.oddsapi.mapping.OddsBookmakerAdapterRegistry;
import net.friendly_bets.oddsapi.mapping.OddsMappingPipeline;
import net.friendly_bets.oddsapi.mapping.XbetOddsAdapter;

import java.util.List;
import java.util.Map;

/**
 * Builds merged {@link OddsMarketGroup} lists via per-bookmaker BetTitle mapping
 * ({@link net.friendly_bets.oddsapi.mapping.OddsMappingPipeline}).
 */
public final class OddsGroupBuilder {

    private static final OddsMappingPipeline PIPELINE = new OddsMappingPipeline(
            new OddsBookmakerAdapterRegistry(new Bet365OddsAdapter(), new XbetOddsAdapter())
    );

    private OddsGroupBuilder() {
    }

    public static List<OddsMarketGroup> build(
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower,
            OddsMatchContext match
    ) {
        return PIPELINE.build(bookmakerMarkets, canonicalByLower, match).getMarketGroups();
    }


    public static net.friendly_bets.oddsapi.mapping.OddsMergeResult buildWithTrace(
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower,
            OddsMatchContext match
    ) {
        return PIPELINE.build(bookmakerMarkets, canonicalByLower, match);
    }
}
