package net.friendly_bets.marathonbet;

/**
 * Какой слот тура синхронизировать в scheduled tick.
 */
public enum MarathonbetSlotScope {
    /** Только текущий тур. */
    CURRENT,
    /** Только следующий тур (если есть). */
    NEXT,
    /** Текущий + следующий в одном тике (ручной / legacy standalone). */
    BOTH
}
