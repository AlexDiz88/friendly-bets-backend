package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.oddsapi.OddsMarketCategory;

/**
 * Какие OK-котировки попадают в prod-merge ({@link OddsMerger}).
 * Форы в prod — только {@link XbetOddsAdapter}; Bet365 handicap-рынки маппятся, но не мержатся.
 */
public final class OddsProductionMergeFilter {

    private OddsProductionMergeFilter() {
    }

    public static boolean includeInProductionMerge(MappedOddsQuote quote) {
        if (quote == null || !quote.isOk()) {
            return true;
        }
        if (quote.getCategory() != OddsMarketCategory.HANDICAP) {
            return true;
        }
        return XbetOddsAdapter.BOOKMAKER.equalsIgnoreCase(quote.getBookmaker());
    }
}
