package net.friendly_bets.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

import static net.friendly_bets.models.enums.BetTitleSubCategory.*;

@Getter
@AllArgsConstructor
public enum BetTitleCategory {
    GAME_RESULTS,
    GAME_RESULT_AND_TOTALS,
    TOTALS,
    HANDICAPS,
    SCORES,
    GOALS,
    HALFTIMES,
    SPECIALS;

    public static final Map<BetTitleCategory, Set<BetTitleSubCategory>> CATEGORY_SUBCATEGORY_MAP = Map.of(
            GAME_RESULTS, Set.of(SUMMARY, HOME_WIN, DRAW, AWAY_WIN, OTHER_RESULTS),
            TOTALS, Set.of(SUMMARY, TOTAL_UNDER, TOTAL_OVER),
            HANDICAPS, Set.of(SUMMARY, HANDICAP_HOME, HANDICAP_AWAY),
            GAME_RESULT_AND_TOTALS, Set.of(SUMMARY, GAME_RESULT_TOTAL_UNDER, GAME_RESULT_TOTAL_OVER),
            SCORES, Set.of(SUMMARY, SCORE_0_0_TO_3_3, SCORE_OTHER),
            GOALS, Set.of(SUMMARY, BOTH_SCORES, BOTH_SCORES_AND_GAME_RESULT, BOTH_SCORES_AND_TOTAL_GOALS, OTHER),
            HALFTIMES, Set.of(SUMMARY, GAME_RESULT, HALF_FULL_FIRST_SECOND, GAME_SCORE, HANDICAP, TOTAL, COMBO_CONDITION, GOALS_HALFTIME),
            SPECIALS, Set.of(SUMMARY, CLEAN_WIN, GOALS_DIFFERENCE, PLAYOFF)
    );
}