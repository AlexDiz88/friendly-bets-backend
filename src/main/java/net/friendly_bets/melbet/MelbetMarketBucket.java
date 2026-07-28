package net.friendly_bets.melbet;

/**
 * Allowlist bucket after Melbet StakeType Id filter (deny-by-default).
 */
public enum MelbetMarketBucket {
    MATCH_RESULT,
    DOUBLE_CHANCE,
    HANDICAP,
    TOTALS,
    TEAM_TOTAL_HOME,
    TEAM_TOTAL_AWAY,
    BTTS,
    RESULT_BTTS,
    RESULT_TOTAL,
    HALF_FULL,
    FIRST_SECOND_HALF,
    CORRECT_SCORE,
    GOALS,
    EXACT_TOTAL_GOALS,
    CLEAN_WIN,
    SCORE_DIFF
}
