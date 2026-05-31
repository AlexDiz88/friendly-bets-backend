package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OddsSelectionBetTitleMapperTest {

    @Test
    @DisplayName("maps match result HOME to HOME_WIN bet title")
    void mapsMatchResultHome() {
        OddsLineRow row = OddsLineRow.builder()
                .selectionCode("HOME")
                .displayLabel("П1")
                .build();

        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle("MATCH_RESULT", row);

        assertEquals(BetTitleCode.HOME_WIN.getCode(), title.getCode());
        assertEquals(BetTitleCode.HOME_WIN.getLabel(), title.getLabel());
        assertFalse(title.isNot());
    }

    @Test
    @DisplayName("maps BTTS NO to BOTH_TEAMS_SCORE with isNot")
    void mapsBttsNo() {
        OddsLineRow row = OddsLineRow.builder()
                .selectionCode("NO")
                .displayLabel("Нет")
                .build();

        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle("BTTS", row);

        assertEquals(BetTitleCode.BOTH_TEAMS_SCORE.getCode(), title.getCode());
        assertEquals(BetTitleCode.BOTH_TEAMS_SCORE.getLabel(), title.getLabel());
        assertEquals(true, title.isNot());
    }

    @Test
    @DisplayName("maps correct score 1-0 to GAME_SCORE_1_0")
    void mapsCorrectScore() {
        OddsLineRow row = OddsLineRow.builder()
                .selectionCode("1-0")
                .displayLabel("1-0")
                .build();

        BetTitle title = OddsSelectionBetTitleMapper.toBetTitle("CORRECT_SCORE", row);

        assertEquals(BetTitleCode.GAME_SCORE_1_0.getCode(), title.getCode());
        assertEquals("Счёт 1:0", title.getLabel());
    }
}
