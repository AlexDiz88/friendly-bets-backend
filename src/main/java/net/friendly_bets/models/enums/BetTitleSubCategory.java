package net.friendly_bets.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BetTitleSubCategory {
    SUMMARY(0),
    HOME_WIN(1), DRAW(2), AWAY_WIN(3), OTHER(4),
    TOTAL_UNDER(5), TOTAL_OVER(6),
    HANDICAP_HOME(7), HANDICAP_AWAY(8),
    GAME_RESULT_TOTAL_UNDER(9), GAME_RESULT_TOTAL_OVER(10),
    SCORE_0_0_TO_3_3(11), SCORE_OTHER(12),
    BOTH_SCORES(13), BOTH_SCORES_AND_GAME_RESULT(14), BOTH_SCORES_AND_TOTAL_GOALS(15),
    CLEAN_WIN(16), GOALS_DIFFERENCE(17), PLAYOFF(18);

    private final int code;
}
