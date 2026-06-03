package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.MarathonbetMarketDto;

/**
 * Рынки Marathonbet, не попадающие в prod-merge (ставки FriendlyBets).
 */
public final class MarathonbetProdMarketFilter {

    private static final String IGNORED_NAME_SUFFIX = " (3 исхода)";

    private MarathonbetProdMarketFilter() {
    }

    public static boolean isIgnoredForProd(MarathonbetMarketDto market) {
        if (market == null || market.getName() == null) {
            return false;
        }
        return market.getName().trim().endsWith(IGNORED_NAME_SUFFIX);
    }

    public static boolean isIgnoredForProd(String marketName) {
        if (marketName == null || marketName.isBlank()) {
            return false;
        }
        return marketName.trim().endsWith(IGNORED_NAME_SUFFIX);
    }
}
