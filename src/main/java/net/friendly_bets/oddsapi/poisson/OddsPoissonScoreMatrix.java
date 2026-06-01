package net.friendly_bets.oddsapi.poisson;

/**
 * Матрица вероятностей счётов (Пуассон + опциональная поправка Dixon–Coles на низкие счёты).
 */
final class OddsPoissonScoreMatrix {

    private static final int MAX_GOALS = 10;

    private final double[][] probabilities;
    private final double tailMass;

    OddsPoissonScoreMatrix(double lambdaHome, double lambdaAway, double rho) {
        probabilities = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        double sum = 0;
        for (int home = 0; home <= MAX_GOALS; home++) {
            for (int away = 0; away <= MAX_GOALS; away++) {
                double p = poisson(home, lambdaHome) * poisson(away, lambdaAway) * dixonColesTau(home, away, lambdaHome, lambdaAway, rho);
                probabilities[home][away] = p;
                sum += p;
            }
        }
        tailMass = Math.max(0, 1.0 - sum);
        if (sum > 0 && sum < 1.0 - 1e-9) {
            double scale = 1.0 / sum;
            for (int home = 0; home <= MAX_GOALS; home++) {
                for (int away = 0; away <= MAX_GOALS; away++) {
                    probabilities[home][away] *= scale;
                }
            }
        }
    }

    double probability(int homeGoals, int awayGoals) {
        if (homeGoals < 0 || awayGoals < 0) {
            return 0;
        }
        if (homeGoals <= MAX_GOALS && awayGoals <= MAX_GOALS) {
            return probabilities[homeGoals][awayGoals];
        }
        return 0;
    }

    double tailMass() {
        return tailMass;
    }

    private static double poisson(int k, double lambda) {
        if (lambda <= 0) {
            return k == 0 ? 1.0 : 0.0;
        }
        return Math.exp(-lambda + k * Math.log(lambda) - logFactorial(k));
    }

    private static double logFactorial(int n) {
        if (n <= 1) {
            return 0;
        }
        double sum = 0;
        for (int i = 2; i <= n; i++) {
            sum += Math.log(i);
        }
        return sum;
    }

    /**
     * @see <a href="https://www.maths.lth.se/matstat/statsfodb/old/publications/MW98.pdf">Dixon &amp; Coles (1997)</a>
     */
    private static double dixonColesTau(int home, int away, double lambdaHome, double lambdaAway, double rho) {
        if (Math.abs(rho) < 1e-9) {
            return 1.0;
        }
        if (home == 0 && away == 0) {
            return 1.0 - lambdaHome * lambdaAway * rho;
        }
        if (home == 0 && away == 1) {
            return 1.0 + lambdaHome * rho;
        }
        if (home == 1 && away == 0) {
            return 1.0 + lambdaAway * rho;
        }
        if (home == 1 && away == 1) {
            return 1.0 - rho;
        }
        return 1.0;
    }
}
