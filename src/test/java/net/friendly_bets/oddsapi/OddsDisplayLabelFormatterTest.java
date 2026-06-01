package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsDisplayLabelFormatterTest {

    @Test
    @DisplayName("formats handicap with space before parenthesis")
    void formatsHandicap() {
        OddsLineRow row = OddsLineRow.builder()
                .line("-2.5")
                .selectionCode("HOME")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HANDICAP_HOME_MINUS_2_5.getCode())
                        .label("Ф1(-2.5)")
                        .build())
                .build();

        assertEquals("Ф1 (-2.5)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, row));
    }

    @Test
    @DisplayName("formats away handicap with inverted sign")
    void formatsAwayHandicap() {
        OddsLineRow row = OddsLineRow.builder()
                .line("2.5")
                .selectionCode("AWAY")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HANDICAP_AWAY_PLUS_2_5.getCode())
                        .label("Ф2(+2.5)")
                        .build())
                .build();

        assertEquals("Ф2 (+2.5)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, row));
    }

    @Test
    @DisplayName("formats raw away handicap from api hdp")
    void formatsRawAwayHandicap() {
        OddsLineRow row = OddsLineRow.builder()
                .line("3.5")
                .selectionCode("AWAY")
                .build();

        assertEquals("Ф2 (+3.5)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, row));
    }

    @Test
    @DisplayName("formats zero handicap line")
    void formatsZeroHandicap() {
        OddsLineRow home = OddsLineRow.builder()
                .line("0")
                .selectionCode("HOME")
                .build();
        OddsLineRow away = OddsLineRow.builder()
                .line("0")
                .selectionCode("AWAY")
                .build();

        assertEquals("Ф1 (0)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, home));
        assertEquals("Ф2 (0)", OddsDisplayLabelFormatter.format(OddsMarketCategory.HANDICAP, away));
    }

    @Test
    @DisplayName("formats totals as ТБ/ТМ with line")
    void formatsTotals() {
        OddsLineRow row = OddsLineRow.builder()
                .line("2.5")
                .selectionCode("OVER")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.TOTAL_OVER_2_5.getCode())
                        .label("ТБ 2.5")
                        .build())
                .build();

        assertEquals("ТБ 2.5", OddsDisplayLabelFormatter.format(OddsMarketCategory.TOTALS, row));
    }

    @Test
    @DisplayName("shortens home team total label")
    void shortensHomeTeamTotal() {
        OddsLineRow row = OddsLineRow.builder()
                .line("1.5")
                .selectionCode("OVER")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.HOME_TEAM_OVER_1_5.getCode())
                        .label("Хозяева ИТБ 1.5")
                        .build())
                .build();

        assertEquals("ИТБ 1.5", OddsDisplayLabelFormatter.format(OddsMarketCategory.TEAM_TOTAL_HOME, row));
    }

    @Test
    @DisplayName("formats BTTS yes and no labels")
    void formatsBttsLabels() {
        OddsLineRow yes = OddsLineRow.builder()
                .selectionCode("YES")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.BOTH_TEAMS_SCORE.getCode())
                        .label("Обе забьют")
                        .isNot(false)
                        .build())
                .build();
        OddsLineRow no = OddsLineRow.builder()
                .selectionCode("NO")
                .betTitle(BetTitle.builder()
                        .code(BetTitleCode.BOTH_TEAMS_SCORE.getCode())
                        .label("Обе забьют")
                        .isNot(true)
                        .build())
                .build();

        assertEquals("Обе забьют", OddsDisplayLabelFormatter.format(OddsMarketCategory.BTTS, yes));
        assertEquals("Обе забьют — нет", OddsDisplayLabelFormatter.format(OddsMarketCategory.BTTS, no));
    }
}
