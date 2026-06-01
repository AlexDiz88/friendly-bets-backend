package net.friendly_bets.oddsapi.poisson;

/**
 * Параметры калибровки двумерного Пуассона по смерженным 1X2 и тоталам.
 */
record OddsPoissonCalibration(double lambdaHome, double lambdaAway, double rho, double margin) {

    /** Маржа комбо ниже суммарной маржи 1X2+тотал (совместная цена уже «толще»). */
    private static final double COMBO_MARGIN_FACTOR = 0.38;

    double marginForCombo(OddsResultTotalCombo combo) {
        return margin * COMBO_MARGIN_FACTOR;
    }

    static OddsPoissonCalibration calibrate(OddsMergedMarketInputs inputs) {
        if (!inputs.isCalibratable()) {
            return null;
        }
        double bestError = Double.MAX_VALUE;
        double bestLh = 1.2;
        double bestLa = 1.0;
        double bestRho = -0.05;

        for (double lh = 0.35; lh <= 3.5; lh += 0.05) {
            for (double la = 0.35; la <= 3.5; la += 0.05) {
                for (double rho : new double[] {-0.2, -0.15, -0.1, -0.05, 0, 0.05, 0.1}) {
                    double err = calibrationError(inputs, lh, la, rho);
                    if (err < bestError) {
                        bestError = err;
                        bestLh = lh;
                        bestLa = la;
                        bestRho = rho;
                    }
                }
            }
        }
        double[] refined = refine(inputs, bestLh, bestLa, bestRho, bestError);
        bestLh = refined[0];
        bestLa = refined[1];
        bestRho = refined[2];

        double margin = (inputs.margin1x2() + inputs.marginTotals()) / 2.0;
        if (margin < 0.02) {
            margin = 0.05;
        }
        margin = Math.min(margin * 1.08, 0.12);
        return new OddsPoissonCalibration(bestLh, bestLa, bestRho, margin);
    }

    private static double[] refine(
            OddsMergedMarketInputs inputs,
            double lh,
            double la,
            double rho,
            double bestError
    ) {
        double step = 0.02;
        for (int pass = 0; pass < 40; pass++) {
            boolean improved = false;
            for (double dlh : new double[] {-step, 0, step}) {
                for (double dla : new double[] {-step, 0, step}) {
                    for (double drho : new double[] {-step * 2, 0, step * 2}) {
                        double nlh = clamp(lh + dlh, 0.2, 4.5);
                        double nla = clamp(la + dla, 0.2, 4.5);
                        double nrho = clamp(rho + drho, -0.2, 0.2);
                        double err = calibrationError(inputs, nlh, nla, nrho);
                        if (err + 1e-12 < bestError) {
                            bestError = err;
                            lh = nlh;
                            la = nla;
                            rho = nrho;
                            improved = true;
                        }
                    }
                }
            }
            if (!improved) {
                step *= 0.5;
                if (step < 0.002) {
                    break;
                }
            }
        }
        return new double[] {lh, la, rho};
    }

    private static double calibrationError(OddsMergedMarketInputs inputs, double lh, double la, double rho) {
        OddsPoissonScoreMatrix matrix = new OddsPoissonScoreMatrix(lh, la, rho);
        double pHome = 0;
        double pDraw = 0;
        double pAway = 0;
        for (int h = 0; h <= 10; h++) {
            for (int a = 0; a <= 10; a++) {
                double p = matrix.probability(h, a);
                if (h > a) {
                    pHome += p;
                } else if (h < a) {
                    pAway += p;
                } else {
                    pDraw += p;
                }
            }
        }
        double err = sq(pHome - inputs.probHomeWin())
                + sq(pDraw - inputs.probDraw())
                + sq(pAway - inputs.probAwayWin());
        double wTotal = 1.4;
        for (var entry : inputs.probOverByLine().entrySet()) {
            double line = entry.getKey();
            double targetOver = entry.getValue();
            double modelOver = modelOverProbability(matrix, line);
            err += wTotal * sq(modelOver - targetOver);
        }
        return err;
    }

    private static double modelOverProbability(OddsPoissonScoreMatrix matrix, double line) {
        double p = 0;
        for (int h = 0; h <= 10; h++) {
            for (int a = 0; a <= 10; a++) {
                int total = h + a;
                double weight = total > line ? 1.0 : (total == line && line == Math.floor(line) ? 0.5 : 0.0);
                p += matrix.probability(h, a) * weight;
            }
        }
        return p;
    }

    private static double sq(double x) {
        return x * x;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    double comboProbability(OddsResultTotalCombo combo) {
        return OddsPoissonComboProbability.jointProbability(this, combo);
    }
}
