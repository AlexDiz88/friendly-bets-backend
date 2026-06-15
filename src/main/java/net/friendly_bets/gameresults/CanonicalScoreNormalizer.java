package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import org.springframework.stereotype.Component;

/**
 * Нормализация канонического счёта FriendlyBets из сырых снимков провайдеров.
 */
@Component
public class CanonicalScoreNormalizer {

    public static final String DURATION_REGULAR = "REGULAR";
    public static final String DURATION_EXTRA_TIME = "EXTRA_TIME";
    public static final String DURATION_PENALTY_SHOOTOUT = "PENALTY_SHOOTOUT";

    /**
     * Нормализация из сырого снимка (recovery после расхождения).
     */
    public GameScore normalizeFromRawSnapshot(GameScore raw, String duration) {
        if (raw == null || raw.getFullTime() == null) {
            return null;
        }
        String normalizedDuration = normalizeDuration(duration);
        GameScore.GameScoreBuilder builder = GameScore.builder().fullTime(raw.getFullTime());

        if (raw.getFirstTime() != null && !raw.getFirstTime().isBlank()) {
            builder.firstTime(raw.getFirstTime());
        }
        if (hasExtraTimePeriod(normalizedDuration)
                && raw.getOverTime() != null
                && !raw.getOverTime().isBlank()) {
            builder.overTime(raw.getOverTime());
        }
        if (DURATION_PENALTY_SHOOTOUT.equals(normalizedDuration)
                && raw.getPenalty() != null
                && !raw.getPenalty().isBlank()) {
            builder.penalty(raw.getPenalty());
            GameScore adjusted = adjustPenaltyFullTime(raw.getFullTime(), raw.getPenalty());
            if (adjusted != null) {
                return adjusted;
            }
        }

        return builder.build();
    }

    public boolean isKnockoutDuration(String duration) {
        return hasExtraTimePeriod(duration) || DURATION_PENALTY_SHOOTOUT.equals(duration);
    }

    static String normalizeDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return DURATION_REGULAR;
        }
        return duration.trim().toUpperCase();
    }

    private static boolean hasExtraTimePeriod(String duration) {
        return DURATION_EXTRA_TIME.equals(duration) || DURATION_PENALTY_SHOOTOUT.equals(duration);
    }

    private static GameScore adjustPenaltyFullTime(String fullTime, String penalty) {
        int[] ft = parseScore(fullTime);
        int[] pen = parseScore(penalty);
        if (ft == null || pen == null) {
            return null;
        }
        int home = ft[0] - pen[0];
        int away = ft[1] - pen[1];
        if (home < 0 || away < 0) {
            return null;
        }
        return GameScore.builder()
                .fullTime(formatScore(home, away))
                .firstTime(null)
                .overTime(fullTime.equals(formatScore(home, away)) ? null : fullTime)
                .penalty(penalty)
                .build();
    }

    private static int[] parseScore(String part) {
        if (part == null || part.isBlank()) {
            return null;
        }
        String[] tokens = part.trim().split(":");
        if (tokens.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatScore(int home, int away) {
        return home + ":" + away;
    }
}
