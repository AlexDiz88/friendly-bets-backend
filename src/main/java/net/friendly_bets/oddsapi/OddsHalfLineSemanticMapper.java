package net.friendly_bets.oddsapi;

import net.friendly_bets.models.enums.BetTitleCode;

import java.util.Optional;

/**
 * Линии 0.5 в API букмекеров семантически совпадают с исходами / «Голами», а не с enum ТБ/ТМ 0.5 или Ф(±0.5).
 */
public final class OddsHalfLineSemanticMapper {

    public record SemanticBet(BetTitleCode code, boolean isNot, OddsMarketCategory displayCategory) {
    }

    private OddsHalfLineSemanticMapper() {
    }

    /** Фора ±0.5 дублирует 1X / П1 / X2 / П2 — не маппим и не логируем. */
    public static boolean isIgnoredHandicapApiLine(String apiLine) {
        if (apiLine == null || apiLine.isBlank()) {
            return false;
        }
        double homeLine = OddsHandicapLine.parse(apiLine);
        return Math.abs(Math.abs(homeLine) - 0.5) < 1e-9;
    }

    public static Optional<SemanticBet> mapMatchTotal(double line, OddsSelectionCode selection) {
        if (!isHalfLine(line) || selection == null) {
            return Optional.empty();
        }
        return switch (selection) {
            case UNDER -> Optional.of(new SemanticBet(
                    BetTitleCode.GAME_SCORE_0_0, false, OddsMarketCategory.GOALS));
            case OVER -> Optional.of(new SemanticBet(
                    BetTitleCode.ANY_TEAM_WILL_SCORE, false, OddsMarketCategory.GOALS));
            default -> Optional.empty();
        };
    }

    public static Optional<SemanticBet> mapTeamTotal(boolean home, double line, OddsSelectionCode selection) {
        if (!isHalfLine(line) || selection == null) {
            return Optional.empty();
        }
        BetTitleCode code = home ? BetTitleCode.HOME_TEAM_SCORES : BetTitleCode.AWAY_TEAM_SCORES;
        return switch (selection) {
            case OVER -> Optional.of(new SemanticBet(code, false, OddsMarketCategory.GOALS));
            case UNDER -> Optional.of(new SemanticBet(code, true, OddsMarketCategory.GOALS));
            default -> Optional.empty();
        };
    }

    private static boolean isHalfLine(double line) {
        return Math.abs(line - 0.5) < 1e-9;
    }
}
