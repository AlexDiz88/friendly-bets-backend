package net.friendly_bets.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static final Map<BetTitleCategory, List<BetTitleSubCategory>> CATEGORY_SUBCATEGORY_MAP =
            new LinkedHashMap<>() {{
                put(GAME_RESULTS, List.of(SUMMARY, HOME_WIN, DRAW, AWAY_WIN, OTHER_RESULTS));
                put(TOTALS, List.of(SUMMARY, TOTAL_UNDER, TOTAL_OVER, PERSONAL_TOTAL));
                put(HANDICAPS, List.of(SUMMARY, HANDICAP_HOME, HANDICAP_AWAY));
                put(GAME_RESULT_AND_TOTALS, List.of(SUMMARY, GAME_RESULT_TOTAL_UNDER, GAME_RESULT_TOTAL_OVER));
                put(SCORES, List.of(SUMMARY, SCORE_0_0_TO_3_3, SCORE_OTHER));
                put(GOALS, List.of(SUMMARY, BOTH_SCORES, BOTH_SCORES_AND_GAME_RESULT, BOTH_SCORES_AND_TOTAL_GOALS, GOALS_IN_HALFTIMES, OTHER));
                put(HALFTIMES, List.of(SUMMARY, GAME_RESULT, HALF_FULL_FIRST_SECOND, GAME_SCORE, HANDICAP, TOTAL, COMBO_CONDITION, GOALS_HALFTIME));
                put(SPECIALS, List.of(SUMMARY, CLEAN_WIN, GOALS_DIFFERENCE, PLAYOFF));
            }};

}