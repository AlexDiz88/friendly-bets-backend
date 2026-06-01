package net.friendly_bets.oddsapi.poisson;

import net.friendly_bets.utils.BetCheckUtils;

/**
 * Совместная вероятность «исход + тотал» с учётом корреляции (модель + сдвиг к маржинальному произведению).
 */
final class OddsPoissonComboProbability {

    /** Сдвиг к max(p_joint, p_indep) — у БК P(комбо) обычно выше «чистого» Пуассона. */
    private static final double OVER_CORRELATION_BLEND = 0.62;
    private static final double UNDER_CORRELATION_BLEND = 0.52;
    /** Общий подъём совместной вероятности (снижает завышенные кэфы). */
    private static final double COMBO_PROBABILITY_BOOST = 1.17;

    private OddsPoissonComboProbability() {
    }

    static double jointProbability(
            OddsPoissonCalibration calibration,
            OddsResultTotalCombo combo
    ) {
        OddsPoissonScoreMatrix matrix = new OddsPoissonScoreMatrix(
                calibration.lambdaHome(), calibration.lambdaAway(), calibration.rho());

        double pJoint = rawJoint(matrix, combo);
        double pResult = marginalResult(matrix, combo.matchResult());
        double pTotal = marginalTotal(matrix, combo.totalType(), combo.totalLine());
        double pIndep = pResult * pTotal;

        double p = adjustForCorrelation(pJoint, pIndep, combo);
        p = Math.min(0.97, p * COMBO_PROBABILITY_BOOST);
        return Math.max(p, 1e-6);
    }

    private static double rawJoint(OddsPoissonScoreMatrix matrix, OddsResultTotalCombo combo) {
        double p = 0;
        for (int h = 0; h <= 10; h++) {
            for (int a = 0; a <= 10; a++) {
                if (!BetCheckUtils.checkBetGameResult(h, a, combo.matchResult())) {
                    continue;
                }
                int total = h + a;
                p += matrix.probability(h, a) * combo.effectiveTotalWeight(total);
            }
        }
        return p;
    }

    private static double marginalResult(OddsPoissonScoreMatrix matrix, BetCheckUtils.MatchResult result) {
        double p = 0;
        for (int h = 0; h <= 10; h++) {
            for (int a = 0; a <= 10; a++) {
                if (BetCheckUtils.checkBetGameResult(h, a, result)) {
                    p += matrix.probability(h, a);
                }
            }
        }
        return p;
    }

    private static double marginalTotal(
            OddsPoissonScoreMatrix matrix,
            BetCheckUtils.TotalType type,
            double line
    ) {
        double p = 0;
        for (int h = 0; h <= 10; h++) {
            for (int a = 0; a <= 10; a++) {
                int total = h + a;
                double w = totalWeight(total, type, line);
                p += matrix.probability(h, a) * w;
            }
        }
        return p;
    }

    private static double totalWeight(int total, BetCheckUtils.TotalType type, double line) {
        boolean integerLine = line == Math.floor(line);
        if (type == BetCheckUtils.TotalType.UNDER) {
            if (total < line) {
                return 1.0;
            }
            if (integerLine && total == line) {
                return 1.0;
            }
            return 0.0;
        }
        if (total > line) {
            return 1.0;
        }
        if (integerLine && total == line) {
            return 1.0;
        }
        return 0.0;
    }

    private static double adjustForCorrelation(
            double pJoint,
            double pIndep,
            OddsResultTotalCombo combo
    ) {
        double blend = combo.totalType() == BetCheckUtils.TotalType.OVER
                ? OVER_CORRELATION_BLEND
                : UNDER_CORRELATION_BLEND;
        double target = Math.max(pJoint, pIndep);
        return pJoint + blend * (target - pJoint);
    }
}
