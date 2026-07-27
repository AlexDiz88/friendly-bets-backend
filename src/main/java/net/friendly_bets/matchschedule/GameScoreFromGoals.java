package net.friendly_bets.matchschedule;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.schedule.MatchGoalEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds {@link GameScore} from goal events only (no scoreboard / status text).
 * <p>
 * Period bucketing uses the <strong>base</strong> minute of the label:
 * <ul>
 *   <li>{@code 45+3} → base 45 → first half (and full time)</li>
 *   <li>{@code 90+6} → base 90 → full time (regulation), not OT</li>
 *   <li>{@code 91+} / {@code 105} / {@code 120+2} → extra time</li>
 * </ul>
 * Penalty shootout goals ({@code penaltyShootout=true}) go only to {@code penalty}.
 * {@code overTime} is the cumulative score after ET (regulation + ET), set only if ET goals exist.
 */
public final class GameScoreFromGoals {

    private static final Pattern BASE_MINUTE = Pattern.compile("(\\d{1,3})");

    private GameScoreFromGoals() {
    }

    public static GameScore from(List<MatchGoalEvent> goals) {
        int htHome = 0, htAway = 0;
        int ftHome = 0, ftAway = 0;
        int otHome = 0, otAway = 0;
        int penHome = 0, penAway = 0;
        boolean anyOt = false;
        boolean anyPen = false;

        if (goals != null) {
            for (MatchGoalEvent goal : goals) {
                if (goal == null) {
                    continue;
                }
                boolean home = "HOME".equalsIgnoreCase(goal.getTeamSide());
                if (Boolean.TRUE.equals(goal.getMissed())) {
                    continue;
                }
                if (Boolean.TRUE.equals(goal.getPenaltyShootout())) {
                    anyPen = true;
                    if (home) {
                        penHome++;
                    } else {
                        penAway++;
                    }
                    continue;
                }
                int base = resolveBaseMinute(goal);
                if (base < 0) {
                    continue;
                }
                if (base <= 45) {
                    if (home) {
                        htHome++;
                    } else {
                        htAway++;
                    }
                }
                if (base <= 90) {
                    if (home) {
                        ftHome++;
                    } else {
                        ftAway++;
                    }
                } else {
                    anyOt = true;
                    if (home) {
                        otHome++;
                    } else {
                        otAway++;
                    }
                }
            }
        }

        GameScore.GameScoreBuilder builder = GameScore.builder()
                .fullTime(formatScore(ftHome, ftAway))
                .firstTime(formatScore(htHome, htAway));
        if (anyOt) {
            builder.overTime(formatScore(ftHome + otHome, ftAway + otAway));
        }
        if (anyPen) {
            builder.penalty(formatScore(penHome, penAway));
        }
        return builder.build();
    }

    /**
     * Base minute for HT/FT/OT bucketing. Injury time keeps the period of the base
     * ({@code 45+n} → 45, {@code 90+n} → 90, {@code 120+n} → 120).
     */
    public static int resolveBaseMinute(MatchGoalEvent goal) {
        if (goal == null) {
            return -1;
        }
        String label = goal.getMinute();
        if (label != null && !label.isBlank()) {
            Matcher m = BASE_MINUTE.matcher(label.trim());
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                    // fall through
                }
            }
        }
        if (goal.getMinuteNumber() != null && goal.getMinuteNumber() >= 0) {
            return goal.getMinuteNumber();
        }
        return -1;
    }

    private static String formatScore(int home, int away) {
        return home + ":" + away;
    }
}
