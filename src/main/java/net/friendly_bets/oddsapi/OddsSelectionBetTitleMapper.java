package net.friendly_bets.oddsapi;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;

import java.util.Locale;
import java.util.Optional;

public final class OddsSelectionBetTitleMapper {

    private OddsSelectionBetTitleMapper() {
    }

    public static BetTitle toBetTitle(String categoryName, OddsLineRow row) {
        OddsMarketCategory category;
        try {
            category = OddsMarketCategory.valueOf(categoryName);
        } catch (Exception e) {
            throw new BadRequestException("betMarketNotAllowedForSelfService");
        }
        if (category == OddsMarketCategory.EXCLUDED
                || category == OddsMarketCategory.OTHER) {
            throw new BadRequestException("betMarketNotAllowedForSelfService");
        }

        String selection = row.getSelectionCode();
        String line = row.getLine();
        BetTitleCode code;
        boolean isNot = false;

        switch (category) {
            case MATCH_RESULT -> code = mapMatchResult(selection);
            case DOUBLE_CHANCE -> code = mapDoubleChance(selection);
            case TOTALS -> code = mapTotal(line, selection);
            case TEAM_TOTAL_HOME -> code = mapTeamTotal(line, selection, true);
            case TEAM_TOTAL_AWAY -> code = mapTeamTotal(line, selection, false);
            case BTTS -> {
                if ("YES".equals(selection)) {
                    code = BetTitleCode.BOTH_TEAMS_SCORE;
                    isNot = false;
                } else if ("NO".equals(selection)) {
                    code = BetTitleCode.BOTH_TEAMS_SCORE;
                    isNot = true;
                } else {
                    throw new BadRequestException("betMarketNotAllowedForSelfService");
                }
                return BetTitle.builder()
                        .code(code.getCode())
                        .label(code.getLabel())
                        .isNot(isNot)
                        .build();
            }
            case HANDICAP -> code = mapHandicap(line, selection);
            case CORRECT_SCORE -> code = mapCorrectScore(row.getSelectionCode());
            default -> throw new BadRequestException("betMarketNotAllowedForSelfService");
        }

        return BetTitle.builder()
                .code(code.getCode())
                .label(code.getLabel())
                .isNot(isNot)
                .build();
    }

    private static BetTitleCode mapMatchResult(String selection) {
        return switch (selection) {
            case "HOME" -> BetTitleCode.HOME_WIN;
            case "DRAW" -> BetTitleCode.DRAW;
            case "AWAY" -> BetTitleCode.AWAY_WIN;
            default -> throw new BadRequestException("betMarketNotAllowedForSelfService");
        };
    }

    private static BetTitleCode mapDoubleChance(String selection) {
        return switch (selection) {
            case "DC_1X" -> BetTitleCode.HOME_WIN_OR_DRAW;
            case "DC_12" -> BetTitleCode.HOME_OR_AWAY_WIN;
            case "DC_X2" -> BetTitleCode.AWAY_WIN_OR_DRAW;
            default -> throw new BadRequestException("betMarketNotAllowedForSelfService");
        };
    }

    private static BetTitleCode mapTotal(String line, String selection) {
        String suffix = lineToSuffix(line);
        boolean over = "OVER".equals(selection);
        String name = "TOTAL_" + (over ? "OVER" : "UNDER") + "_" + suffix;
        return findEnum(name).orElseThrow(() -> new BadRequestException("betMarketNotAllowedForSelfService"));
    }

    private static BetTitleCode mapTeamTotal(String line, String selection, boolean home) {
        String suffix = lineToSuffix(line);
        boolean over = "OVER".equals(selection);
        String prefix = home ? "HOME_TEAM" : "AWAY_TEAM";
        String name = prefix + "_" + (over ? "OVER" : "UNDER") + "_" + suffix;
        return findEnum(name).orElseThrow(() -> new BadRequestException("betMarketNotAllowedForSelfService"));
    }

    private static BetTitleCode mapCorrectScore(String selection) {
        int[] score = OddsCorrectScoreUtils.parseScore(selection);
        if (score == null) {
            throw new BadRequestException("betMarketNotAllowedForSelfService");
        }
        String name = "GAME_SCORE_" + score[0] + "_" + score[1];
        return findEnum(name).orElseThrow(() -> new BadRequestException("betMarketNotAllowedForSelfService"));
    }

    private static BetTitleCode mapHandicap(String line, String selection) {
        boolean home = "HOME".equals(selection);
        double effective = OddsHandicapLine.effectiveLine(line, home);
        String suffix = handicapSuffix(effective, home);
        String prefix = home ? "HANDICAP_HOME" : "HANDICAP_AWAY";
        return findEnum(prefix + suffix)
                .orElseThrow(() -> new BadRequestException("betMarketNotAllowedForSelfService"));
    }

    private static String handicapSuffix(double effectiveLine, boolean home) {
        if (Math.abs(effectiveLine) < 1e-9) {
            return "_0";
        }
        String sign = effectiveLine > 0 ? "PLUS" : "MINUS";
        String value = formatLine(Math.abs(effectiveLine));
        return "_" + sign + "_" + value;
    }

    private static String lineToSuffix(String line) {
        return formatLine(parseLine(line));
    }

    private static String formatLine(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value).replace('.', '_');
    }

    private static double parseLine(String line) {
        if (line == null || line.isBlank()) {
            return 0;
        }
        return Double.parseDouble(line.trim().replace(',', '.'));
    }

    private static Optional<BetTitleCode> findEnum(String name) {
        try {
            return Optional.of(BetTitleCode.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
