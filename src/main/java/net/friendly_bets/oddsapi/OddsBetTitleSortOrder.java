package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;

import java.util.Comparator;

/**
 * Порядок строк: обе команды → хозяева → гости → любая; затем код; «Да» перед «Нет».
 */
public final class OddsBetTitleSortOrder {

    public static final Comparator<OddsLineRow> BY_TEAM_SCOPE = Comparator
            .comparingInt(OddsBetTitleSortOrder::teamScopeKeyForRow)
            .thenComparingInt(OddsBetTitleSortOrder::betTitleCodeKey)
            .thenComparingInt(OddsBetTitleSortOrder::yesBeforeNoKey);

    /** Плей-офф: выход (5212–5217) → остальное → «12» (5205, 5208, 5211). */
    public static final Comparator<OddsLineRow> BY_PLAYOFF = Comparator
            .comparingInt(OddsBetTitleSortOrder::playoffTierKeyForRow)
            .thenComparingInt(OddsBetTitleSortOrder::betTitleCodeKey)
            .thenComparingInt(OddsBetTitleSortOrder::yesBeforeNoKey);

    private OddsBetTitleSortOrder() {
    }

    /** 0 = выход/победитель (QLF), 1 = прочее, 2 = «12». */
    static int playoffTierKeyForRow(OddsLineRow row) {
        BetTitleCode code = betTitleCode(row);
        if (code == null) {
            return 1;
        }
        return switch (code) {
            case PLAYOFF_HOME_ADVANCE_NEXT_STAGE, PLAYOFF_AWAY_ADVANCE_NEXT_STAGE,
                 PLAYOFF_HOME_ADVANCE_FINAL, PLAYOFF_AWAY_ADVANCE_FINAL,
                 PLAYOFF_HOME_WIN_TOURNAMENT, PLAYOFF_AWAY_WIN_TOURNAMENT -> 0;
            case PLAYOFF_HOME_OR_AWAY_REGULAR, PLAYOFF_HOME_OR_AWAY_OVERTIME,
                 PLAYOFF_HOME_OR_AWAY_PENALTIES -> 2;
            default -> 1;
        };
    }

    /** 0 = обе, 1 = хозяева, 2 = гости, 3 = любая, 4 = прочее. */
    static int teamScopeKeyForRow(OddsLineRow row) {
        BetTitleCode code = betTitleCode(row);
        if (code == null) {
            return 4;
        }
        return teamScopeKeyForCode(code);
    }

    static int teamScopeKeyForCode(BetTitleCode code) {
        return switch (code) {
            case BOTH_TEAMS_SCORE, BOTH_TEAMS_SCORE_1ST_HALF, BOTH_TEAMS_SCORE_2ND_HALF,
                 BOTH_TEAMS_SCORE_BOTH_HALVES, GOALS_IN_BOTH_HALVES,
                 HOME_WIN_AND_BOTH_TEAMS_SCORE, DRAW_AND_BOTH_TEAMS_SCORE,
                 AWAY_WIN_AND_BOTH_TEAMS_SCORE, HOME_OR_DRAW_AND_BOTH_TEAMS_SCORE,
                 AWAY_OR_DRAW_AND_BOTH_TEAMS_SCORE -> 0;
            case HOME_TEAM_SCORES, HOME_SCORES_1ST_HALF, HOME_SCORES_2ND_HALF, HOME_SCORES_BOTH_HALVES,
                 CLEAN_WIN_HOME,
                 GOALS_DIFF_HOME_WIN_1, GOALS_DIFF_HOME_WIN_2, GOALS_DIFF_HOME_WIN_3 -> 1;
            case AWAY_TEAM_SCORES, AWAY_SCORES_1ST_HALF, AWAY_SCORES_2ND_HALF, AWAY_SCORES_BOTH_HALVES,
                 CLEAN_WIN_AWAY,
                 GOALS_DIFF_AWAY_WIN_1, GOALS_DIFF_AWAY_WIN_2, GOALS_DIFF_AWAY_WIN_3 -> 2;
            case ANY_TEAM_WILL_SCORE, ANY_TEAM_SCORES_2_OR_MORE, ANY_TEAM_SCORES_3_OR_MORE,
                 ANY_TEAM_SCORES_4_OR_MORE, ANY_TEAM_SCORES_5_OR_MORE, CLEAN_WIN_ANY,
                 GOALS_DIFF_HOME_OR_AWAY_WIN_1, GOALS_DIFF_HOME_OR_AWAY_WIN_2, GOALS_DIFF_HOME_OR_AWAY_WIN_3,
                 HOME_OR_AWAY_AND_BOTH_TEAMS_SCORE -> 3;
            default -> fallbackTeamScope(code);
        };
    }

    private static int fallbackTeamScope(BetTitleCode code) {
        String name = code.name();
        if (name.contains("BOTH")) {
            return 0;
        }
        if (name.startsWith("HOME_") || name.contains("GOALS_DIFF_HOME_WIN")) {
            return 1;
        }
        if (name.startsWith("AWAY_") || name.contains("GOALS_DIFF_AWAY_WIN")) {
            return 2;
        }
        if (name.startsWith("ANY_") || name.contains("HOME_OR_AWAY") || name.contains("_OR_AWAY_WIN")) {
            return 3;
        }
        return 4;
    }

    static int betTitleCodeKey(OddsLineRow row) {
        BetTitleCode code = betTitleCode(row);
        return code != null ? code.getCode() : Integer.MAX_VALUE;
    }

    /** «Да» (isNot=false) раньше «Нет». */
    static int yesBeforeNoKey(OddsLineRow row) {
        BetTitle betTitle = row.getBetTitle();
        if (betTitle == null) {
            return 0;
        }
        return betTitle.isNot() ? 1 : 0;
    }

    private static BetTitleCode betTitleCode(OddsLineRow row) {
        if (row == null || row.getBetTitle() == null) {
            return null;
        }
        return BetTitleCode.fromCode(row.getBetTitle().getCode());
    }
}
