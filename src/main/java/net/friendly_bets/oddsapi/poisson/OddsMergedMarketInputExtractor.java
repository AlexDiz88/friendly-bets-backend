package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Извлекает fair-вероятности из смерженных групп {@link OddsMarketCategory#MATCH_RESULT} и {@link OddsMarketCategory#TOTALS}.
 */
public final class OddsMergedMarketInputExtractor {

    private OddsMergedMarketInputExtractor() {
    }

    public static OddsMergedMarketInputs extract(List<OddsMarketGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return OddsMergedMarketInputs.empty();
        }
        double[] matchImplied = new double[3];
        double matchSum = 0;
        Map<Double, double[]> totalsByLine = new LinkedHashMap<>();

        for (OddsMarketGroup group : groups) {
            if (group.getRows() == null) {
                continue;
            }
            OddsMarketCategory category;
            try {
                category = OddsMarketCategory.valueOf(group.getCategory());
            } catch (Exception e) {
                continue;
            }
            if (category == OddsMarketCategory.MATCH_RESULT) {
                for (OddsLineRow row : group.getRows()) {
                    double implied = impliedFromBestOdds(row);
                    if (implied <= 0) {
                        continue;
                    }
                    switch (row.getSelectionCode()) {
                        case "HOME" -> {
                            matchImplied[0] = implied;
                            matchSum += implied;
                        }
                        case "DRAW" -> {
                            matchImplied[1] = implied;
                            matchSum += implied;
                        }
                        case "AWAY" -> {
                            matchImplied[2] = implied;
                            matchSum += implied;
                        }
                        default -> { }
                    }
                }
            } else if (category == OddsMarketCategory.TOTALS) {
                for (OddsLineRow row : group.getRows()) {
                    double implied = impliedFromBestOdds(row);
                    if (implied <= 0 || row.getLine() == null || row.getLine().isBlank()) {
                        continue;
                    }
                    double line = parseLine(row.getLine());
                    totalsByLine.putIfAbsent(line, new double[2]);
                    double[] pair = totalsByLine.get(line);
                    if ("OVER".equals(row.getSelectionCode())) {
                        pair[0] = implied;
                    } else if ("UNDER".equals(row.getSelectionCode())) {
                        pair[1] = implied;
                    }
                }
            }
        }

        double margin1x2 = matchSum > 1 ? matchSum - 1 : 0;
        double pHome = matchSum > 0 ? matchImplied[0] / matchSum : 0;
        double pDraw = matchSum > 0 ? matchImplied[1] / matchSum : 0;
        double pAway = matchSum > 0 ? matchImplied[2] / matchSum : 0;

        Map<Double, Double> probOver = new LinkedHashMap<>();
        double marginTotalsAcc = 0;
        int marginTotalsCount = 0;
        for (Map.Entry<Double, double[]> entry : totalsByLine.entrySet()) {
            double over = entry.getValue()[0];
            double under = entry.getValue()[1];
            if (over <= 0 || under <= 0) {
                continue;
            }
            double sum = over + under;
            marginTotalsAcc += sum - 1;
            marginTotalsCount++;
            probOver.put(entry.getKey(), over / sum);
        }
        double marginTotals = marginTotalsCount > 0 ? marginTotalsAcc / marginTotalsCount : 0;

        return new OddsMergedMarketInputs(pHome, pDraw, pAway, probOver, margin1x2, marginTotals);
    }

    private static double impliedFromBestOdds(OddsLineRow row) {
        if (row.getBestOdds() == null || row.getBestOdds().isBlank() || "—".equals(row.getBestOdds())) {
            return 0;
        }
        try {
            double odds = Double.parseDouble(row.getBestOdds().trim().replace(',', '.'));
            return odds > 1 ? 1.0 / odds : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseLine(String line) {
        return Double.parseDouble(line.trim().replace(',', '.'));
    }
}
