package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.oddsapi.OddsMarketCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сверка фор 1xbet (prod) с Bet365 handicap-рынками (Alternative Asian Handicap, Spread, …).
 * Bet365 не участвует в prod-merge, но расхождения логируются для мониторинга качества API.
 */
public final class OddsHandicapMonitor {

    private OddsHandicapMonitor() {
    }

    public static List<OddsCrossBookmakerMismatch> detect(List<MappedOddsQuote> allQuotes) {
        if (allQuotes == null || allQuotes.isEmpty()) {
            return List.of();
        }
        Map<BetTitleKey, MappedOddsQuote> primary = indexHandicap(allQuotes, XbetOddsAdapter.BOOKMAKER);
        Map<BetTitleKey, MappedOddsQuote> monitor = indexHandicap(allQuotes, Bet365OddsAdapter.BOOKMAKER);
        if (primary.isEmpty() || monitor.isEmpty()) {
            return List.of();
        }

        List<OddsCrossBookmakerMismatch> mismatches = new ArrayList<>();
        for (Map.Entry<BetTitleKey, MappedOddsQuote> entry : monitor.entrySet()) {
            MappedOddsQuote primaryQuote = primary.get(entry.getKey());
            if (primaryQuote == null) {
                continue;
            }
            MappedOddsQuote monitorQuote = entry.getValue();
            Map<String, String> pair = Map.of(
                    primaryQuote.getBookmaker(), primaryQuote.getOdds(),
                    monitorQuote.getBookmaker(), monitorQuote.getOdds()
            );
            if (!OddsMappingValidator.isCrossBookmakerMismatch(pair)) {
                continue;
            }
            OddsCrossBookmakerMismatch mismatch = OddsMappingValidator.firstMismatch(
                    entry.getKey(),
                    List.of(primaryQuote, monitorQuote));
            if (mismatch != null) {
                mismatches.add(mismatch);
            }
        }
        return mismatches;
    }

    private static Map<BetTitleKey, MappedOddsQuote> indexHandicap(
            List<MappedOddsQuote> quotes,
            String bookmaker
    ) {
        Map<BetTitleKey, MappedOddsQuote> byKey = new LinkedHashMap<>();
        for (MappedOddsQuote quote : quotes) {
            if (!quote.isOk()
                    || quote.getCategory() != OddsMarketCategory.HANDICAP
                    || quote.getBookmaker() == null
                    || !bookmaker.equalsIgnoreCase(quote.getBookmaker())) {
                continue;
            }
            BetTitleKey key = quote.betTitleKey();
            if (key == null) {
                continue;
            }
            byKey.put(key, quote);
        }
        return byKey;
    }
}
