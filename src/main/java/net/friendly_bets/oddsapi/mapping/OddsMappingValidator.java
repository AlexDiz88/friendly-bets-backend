package net.friendly_bets.oddsapi.mapping;

import java.util.List;
import java.util.Map;

public final class OddsMappingValidator {

    /**
     * Относительное расхождение (max−min) / mid выше порога → mismatch (fail-closed).
     * Без абсолютного порога: на высоких кэфах (точный счёт, аутсайдер) разница > 1.0 бывает при нормальной линии.
     */
    private static final double RELATIVE_MISMATCH_THRESHOLD = 0.50;

    private OddsMappingValidator() {
    }

    public static boolean isCrossBookmakerMismatch(Map<String, String> bookmakerOdds) {
        if (bookmakerOdds == null || bookmakerOdds.size() < 2) {
            return false;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int count = 0;
        for (String raw : bookmakerOdds.values()) {
            double v = parseOdds(raw);
            if (v <= 0) {
                continue;
            }
            count++;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (count < 2) {
            return false;
        }
        double mid = (min + max) / 2.0;
        if (mid < 1e-9) {
            return false;
        }
        return (max - min) / mid > RELATIVE_MISMATCH_THRESHOLD;
    }

    public static OddsCrossBookmakerMismatch firstMismatch(
            BetTitleKey key,
            List<MappedOddsQuote> quotes
    ) {
        if (quotes == null || quotes.size() < 2) {
            return null;
        }
        MappedOddsQuote a = quotes.get(0);
        for (int i = 1; i < quotes.size(); i++) {
            MappedOddsQuote b = quotes.get(i);
            Map<String, String> pair = Map.of(
                    a.getBookmaker(), a.getOdds(),
                    b.getBookmaker(), b.getOdds()
            );
            if (isCrossBookmakerMismatch(pair)) {
                return OddsCrossBookmakerMismatch.builder()
                        .betTitleKey(key)
                        .betTitle(a.getBetTitle())
                        .bookmakerA(a.getBookmaker())
                        .oddsA(a.getOdds())
                        .bookmakerB(b.getBookmaker())
                        .oddsB(b.getOdds())
                        .build();
            }
        }
        return null;
    }

    private static double parseOdds(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
