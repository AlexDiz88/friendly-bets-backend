package net.friendly_bets.oddsapi.poisson;

import java.util.Collections;
import java.util.Map;

/**
 * Де-vig вероятности из смерженных рынков 1X2 и тоталов (best odds).
 */
public record OddsMergedMarketInputs(
        double probHomeWin,
        double probDraw,
        double probAwayWin,
        Map<Double, Double> probOverByLine,
        double margin1x2,
        double marginTotals
) {

    public OddsMergedMarketInputs {
        probOverByLine = probOverByLine != null ? Map.copyOf(probOverByLine) : Map.of();
    }

    public boolean isCalibratable() {
        return probHomeWin > 0 && probDraw > 0 && probAwayWin > 0 && !probOverByLine.isEmpty();
    }

    public static OddsMergedMarketInputs empty() {
        return new OddsMergedMarketInputs(0, 0, 0, Collections.emptyMap(), 0, 0);
    }
}
