package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.MarathonbetMarketDto;

import java.util.Locale;
import java.util.Set;

/**
 * Рынки Marathonbet, не попадающие в prod-merge (ставки FriendlyBets).
 */
public final class MarathonbetProdMarketFilter {

    private static final String IGNORED_NAME_SUFFIX = " (3 исхода)";

    private static final Set<String> IGNORED_MODELS = Set.of(
            "MTCH_TEWFB",
            "MTCH_T1WFB",
            "MTCH_T1NGI",
            "MTCH_T2NGI",
            "MTCH_T1OR2NGUND"
    );

    private MarathonbetProdMarketFilter() {
    }

    public static boolean isIgnoredForProd(MarathonbetMarketDto market) {
        if (market == null) {
            return false;
        }
        return isIgnoredForProd(market.getModel(), market.getName());
    }

    public static boolean isIgnoredForProd(String marketName) {
        return isIgnoredForProd(null, marketName);
    }

    public static boolean isIgnoredForProd(String model, String marketName) {
        if (model != null) {
            if (IGNORED_MODELS.contains(model)) {
                return true;
            }
            if (model.contains("_ASN")) {
                return true;
            }
            if (model.contains("NGI") || model.endsWith("WFB")) {
                return true;
            }
        }
        if (marketName == null || marketName.isBlank()) {
            return false;
        }
        String trimmed = marketName.trim();
        if (trimmed.endsWith(IGNORED_NAME_SUFFIX)) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("азиатск")) {
            return true;
        }
        return lower.contains("подряд");
    }
}
