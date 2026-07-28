package net.friendly_bets.odds.mapping;

import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.melbet.MelbetBookmaker;
import net.friendly_bets.odds.OddsMarketCategory;

/**
 * Какие OK-котировки попадают в prod-merge ({@link OddsMerger}).
 * Форы — только Marathonbet / Melbet (европейская фора); odds-api Spread/Asian не используются.
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
        String bookmaker = quote.getBookmaker();
        return MarathonbetBookmaker.KEY.equalsIgnoreCase(bookmaker)
                || MelbetBookmaker.KEY.equalsIgnoreCase(bookmaker);
    }
}
