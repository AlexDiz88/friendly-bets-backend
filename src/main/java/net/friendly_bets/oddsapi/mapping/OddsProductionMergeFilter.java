package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.oddsapi.OddsMarketCategory;

/**
 * Какие OK-котировки попадают в prod-merge ({@link OddsMerger}).
 * Форы — только Marathonbet (европейская «Победа с учётом форы»); odds-api Spread/Asian не используются.
 */
public final class OddsProductionMergeFilter {

    private OddsProductionMergeFilter() {
    }

    public static boolean includeInProductionMerge(MappedOddsQuote quote) {
        if (quote == null || !quote.isOk()) {
            return true;
        }
        if (quote.getCategory() != OddsMarketCategory.HANDICAP
                && quote.getCategory() != OddsMarketCategory.PERIOD_HANDICAP) {
            return true;
        }
        return MarathonbetBookmaker.KEY.equalsIgnoreCase(quote.getBookmaker());
    }
}
