package net.friendly_bets.validation.betcheckers;

import net.friendly_bets.dto.GameScores;
import net.friendly_bets.models.Bet.BetStatus;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.utils.BetCheckUtils;

public class SpecialBetsChecker implements BetChecker {

    @Override
    public BetStatus check(GameScore gameScore, BetTitleCode code) {
        GameScores gameScores = BetCheckUtils.parse(gameScore);
        int home = gameScores.getHomeFullTime();
        int homeOvertime = gameScores.getHomeOverTime();
        int homePenalty = gameScores.getHomePenalty();

        int away = gameScores.getAwayFullTime();
        int awayOvertime = gameScores.getAwayOverTime();
        int awayPenalty = gameScores.getAwayPenalty();

        boolean isOverTime = checkExtraTime(gameScore.getOverTime());
        boolean isPenalty = checkExtraTime(gameScore.getPenalty());
        boolean homeAny = home > away || homeOvertime > awayOvertime || homePenalty > awayPenalty;
        boolean awayAny = home < away || homeOvertime < awayOvertime || homePenalty < awayPenalty;
        Boolean homeAdvanced = getPlayoffWinner(isOverTime, isPenalty, homeOvertime, awayOvertime, homePenalty, awayPenalty);

        return switch (code) {
            case CLEAN_WIN_HOME -> home > away && away == 0 ? BetStatus.WON : BetStatus.LOST;
            case CLEAN_WIN_AWAY -> home < away && home == 0 ? BetStatus.WON : BetStatus.LOST;
            case CLEAN_WIN_ANY -> (home > away && away == 0) || (home < away && home == 0) ? BetStatus.WON : BetStatus.LOST;

            case GOALS_DIFF_HOME_WIN_1 -> (home - away == 1) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_AWAY_WIN_1 -> (away - home == 1) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_HOME_WIN_2 -> (home - away == 2) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_AWAY_WIN_2 -> (away - home == 2) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_HOME_WIN_3 -> (home - away == 3) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_AWAY_WIN_3 -> (away - home == 3) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_HOME_OR_AWAY_WIN_1 -> (home - away == 1) || (away - home == 1) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_HOME_OR_AWAY_WIN_2 -> (home - away == 2) || (away - home == 2) ? BetStatus.WON : BetStatus.LOST;
            case GOALS_DIFF_HOME_OR_AWAY_WIN_3 -> (home - away == 3) || (away - home == 3) ? BetStatus.WON : BetStatus.LOST;

            case PLAYOFF_EXTRA_TIME -> isOverTime ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_PENALTIES -> isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_WIN_REGULAR -> home > away && !isOverTime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_WIN_REGULAR -> home < away && !isOverTime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_OR_AWAY_REGULAR -> home != away && !isOverTime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_WIN_OVERTIME -> homeOvertime > awayOvertime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_WIN_OVERTIME -> homeOvertime < awayOvertime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_OR_AWAY_OVERTIME -> homeOvertime != awayOvertime && !isPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_WIN_PENALTIES -> homePenalty > awayPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_WIN_PENALTIES -> homePenalty < awayPenalty ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_OR_AWAY_PENALTIES -> homePenalty != awayPenalty ? BetStatus.WON : BetStatus.LOST;

            // TODO: нужно придумать систему авто-определения результата ставок "команда выйдет в след.стадию/в финал". Текущая система учитывает только результат одной игры а не серии.
            case PLAYOFF_HOME_ADVANCE_NEXT_STAGE -> homeAdvanced == null ? BetStatus.OPENED : homeAdvanced ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_ADVANCE_NEXT_STAGE -> homeAdvanced == null ? BetStatus.OPENED : !homeAdvanced ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_ADVANCE_FINAL -> homeAdvanced == null ? BetStatus.OPENED : homeAdvanced ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_ADVANCE_FINAL -> homeAdvanced == null ? BetStatus.OPENED : !homeAdvanced ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_HOME_WIN_TOURNAMENT -> homeAny ? BetStatus.WON : BetStatus.LOST;
            case PLAYOFF_AWAY_WIN_TOURNAMENT -> awayAny ? BetStatus.WON : BetStatus.LOST;

            default -> throw new IllegalArgumentException("Unsupported code: " + code);
        };
    }

    private boolean checkExtraTime(String extraTime) {
        return extraTime != null && !extraTime.isBlank();
    }

    private Boolean getPlayoffWinner(boolean isOverTime, boolean isPenalty, int homeOT, int awayOT, int homePen, int awayPen) {
        if (isPenalty) {
            if (homePen > awayPen) return true;
            if (homePen < awayPen) return false;
            return null;
        }
        if (isOverTime) {
            if (homeOT > awayOT) return true;
            if (homeOT < awayOT) return false;
            return null;
        }
        return null; // невозможно определить
    }
}
