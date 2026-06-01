package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.utils.BetCheckUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Расчёт кэфов «Результат + Тотал» по смерженным 1X2 и тоталам (модель Пуассона).
 */
public final class OddsResultTotalCalculator {

    private static final double MIN_ODDS = 1.01;
    private static final double MAX_ODDS = 100.0;

    private OddsResultTotalCalculator() {
    }

    /**
     * Две группы: {@link OddsMarketCategory#RESULT_TOTAL_OVER} и {@link OddsMarketCategory#RESULT_TOTAL_UNDER}.
     */
    public static List<OddsMarketGroup> buildGroups(List<OddsMarketGroup> mergedGroups, List<String> bookmakers) {
        OddsMergedMarketInputs inputs = OddsMergedMarketInputExtractor.extract(mergedGroups);
        if (!inputs.isCalibratable()) {
            return List.of();
        }
        OddsPoissonCalibration calibration = OddsPoissonCalibration.calibrate(inputs);
        if (calibration == null) {
            return List.of();
        }

        List<OddsLineRow> overRows = new ArrayList<>();
        List<OddsLineRow> underRows = new ArrayList<>();
        for (BetTitleCode code : BetTitleCode.values()) {
            OddsResultTotalCombo combo = OddsResultTotalCombo.fromBetTitleCode(code);
            if (combo == null) {
                continue;
            }
            if (!hasTotalLineInInputs(inputs, combo.totalLine())) {
                continue;
            }
            double prob = calibration.comboProbability(combo);
            double odds = (1.0 + calibration.marginForCombo(combo)) / prob;
            odds = clampOdds(odds);
            String oddsStr = formatOdds(odds);
            Map<String, String> bkOdds = buildBookmakerOdds(bookmakers, oddsStr);
            BetTitle betTitle = BetTitle.builder()
                    .code(code.getCode())
                    .label(code.getLabel())
                    .isNot(false)
                    .build();
            OddsLineRow row = OddsLineRow.builder()
                    .line(formatLine(combo.totalLine()))
                    .selectionCode(combo.betTitleCode().name())
                    .displayLabel(code.getLabel())
                    .betTitle(betTitle)
                    .bookmakerOdds(bkOdds)
                    .bestOdds(oddsStr)
                    .bestBookmaker(bookmakers != null && !bookmakers.isEmpty() ? bookmakers.get(0) : "calc")
                    .build();
            if (OddsResultTotalCombo.isOverComboCode(code.getCode())) {
                overRows.add(row);
            } else {
                underRows.add(row);
            }
        }

        List<OddsMarketGroup> groups = new ArrayList<>();
        if (!overRows.isEmpty()) {
            overRows.sort(Comparator.comparingInt(r -> r.getBetTitle().getCode()));
            groups.add(toMarketGroup(OddsMarketCategory.RESULT_TOTAL_OVER, overRows));
        }
        if (!underRows.isEmpty()) {
            underRows.sort(Comparator.comparingInt(r -> r.getBetTitle().getCode()));
            groups.add(toMarketGroup(OddsMarketCategory.RESULT_TOTAL_UNDER, underRows));
        }
        return groups;
    }

    private static OddsMarketGroup toMarketGroup(OddsMarketCategory category, List<OddsLineRow> rows) {
        return OddsMarketGroup.builder()
                .category(category.name())
                .groupKey(OddsMarketCatalog.i18nGroupKey(category))
                .sortOrder(category.getSortOrder())
                .collapsedByDefault(category.isCollapsedByDefault())
                .rows(rows)
                .build();
    }

    private static boolean hasTotalLineInInputs(OddsMergedMarketInputs inputs, double line) {
        for (Double key : inputs.probOverByLine().keySet()) {
            if (Math.abs(key - line) < 1e-6) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildBookmakerOdds(List<String> bookmakers, String oddsStr) {
        Map<String, String> map = new LinkedHashMap<>();
        if (bookmakers != null) {
            for (String bk : bookmakers) {
                if (bk != null && !bk.isBlank()) {
                    map.put(bk, oddsStr);
                }
            }
        }
        if (map.isEmpty()) {
            map.put("calc", oddsStr);
        }
        return map;
    }

    private static double clampOdds(double odds) {
        return Math.max(MIN_ODDS, Math.min(MAX_ODDS, odds));
    }

    private static String formatOdds(double odds) {
        if (odds >= 10) {
            return String.format(Locale.US, "%.2f", odds);
        }
        if (odds >= 2) {
            return String.format(Locale.US, "%.2f", odds);
        }
        return String.format(Locale.US, "%.3f", odds);
    }

    private static String formatLine(double line) {
        if (line == Math.floor(line)) {
            return String.valueOf((int) line);
        }
        return String.valueOf(line);
    }
}
