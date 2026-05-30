package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsSelectionBetTitleMapperTest {

    @Test
    void mapsMatchResultHome() {
        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle(
                OddsMarketCategory.MATCH_RESULT.name(),
                OddsLineRow.builder().selectionCode("HOME").build());
        assertEquals(BetTitleCode.HOME_WIN.getCode(), title.getCode());
    }

    @Test
    void mapsTotalsOver25() {
        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle(
                OddsMarketCategory.TOTALS.name(),
                OddsLineRow.builder().selectionCode("OVER").line("2.5").build());
        assertEquals(BetTitleCode.TOTAL_OVER_2_5.getCode(), title.getCode());
    }

    @Test
    void mapsBttsNoWithIsNot() {
        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle(
                OddsMarketCategory.BTTS.name(),
                OddsLineRow.builder().selectionCode("NO").build());
        assertEquals(BetTitleCode.BOTH_TEAMS_SCORE.getCode(), title.getCode());
        assertTrue(title.isNot());
    }

    @Test
    void mapsDoubleChance1x() {
        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle(
                OddsMarketCategory.DOUBLE_CHANCE.name(),
                OddsLineRow.builder().selectionCode("DC_1X").build());
        assertEquals(BetTitleCode.HOME_WIN_OR_DRAW.getCode(), title.getCode());
        assertFalse(title.isNot());
    }
}
