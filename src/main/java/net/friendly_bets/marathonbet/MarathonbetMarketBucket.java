package net.friendly_bets.marathonbet;

/**
 * Категория рынка Marathonbet после allowlist-фильтрации (deny-by-default).
 */
public enum MarathonbetMarketBucket {
    MATCH_RESULT,
    HALF_TIME_RESULT,
    SECOND_HALF_RESULT,
    DOUBLE_CHANCE,
    HALF_TIME_DOUBLE_CHANCE,
    SECOND_HALF_DOUBLE_CHANCE,
    HANDICAP,
    HALF_TIME_HANDICAP,
    SECOND_HALF_HANDICAP,
    TOTALS,
    HALF_TIME_TOTALS,
    SECOND_HALF_TOTALS,
    TEAM_TOTAL_HOME,
    TEAM_TOTAL_AWAY,
    CORRECT_SCORE,
    FIRST_HALF_CORRECT_SCORE,
    SECOND_HALF_CORRECT_SCORE,
    RESULT_TOTAL,
    GOALS,
    CLEAN_WIN,
    SCORE_DIFF,
    HALF_FULL,
    FIRST_SECOND_HALF,
    BTTS_RESULT,
    /** Выход / доп. время / пенальти — только нокаут-матчи. */
    PLAYOFF
}
