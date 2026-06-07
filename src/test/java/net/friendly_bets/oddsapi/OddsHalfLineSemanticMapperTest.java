package net.friendly_bets.oddsapi;

import net.friendly_bets.models.enums.BetTitleCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsHalfLineSemanticMapperTest {

    @Test
    void ignoresHandicapHalfLines() {
        assertTrue(OddsHalfLineSemanticMapper.isIgnoredHandicapApiLine("0.5"));
        assertTrue(OddsHalfLineSemanticMapper.isIgnoredHandicapApiLine("-0.5"));
        assertFalse(OddsHalfLineSemanticMapper.isIgnoredHandicapApiLine("1.5"));
    }

    @Test
    void mapsMatchTotalHalfLine() {
        var under = OddsHalfLineSemanticMapper.mapMatchTotal(0.5, OddsSelectionCode.UNDER).orElseThrow();
        assertEquals(BetTitleCode.GAME_SCORE_0_0, under.code());
        assertFalse(under.isNot());
        assertEquals(OddsMarketCategory.CORRECT_SCORE, under.displayCategory());

        var over = OddsHalfLineSemanticMapper.mapMatchTotal(0.5, OddsSelectionCode.OVER).orElseThrow();
        assertEquals(BetTitleCode.ANY_TEAM_WILL_SCORE, over.code());
        assertFalse(over.isNot());
        assertEquals(OddsMarketCategory.GOALS, over.displayCategory());
    }

    @Test
    void mapsTeamTotalHalfLine() {
        var homeOver = OddsHalfLineSemanticMapper.mapTeamTotal(true, 0.5, OddsSelectionCode.OVER).orElseThrow();
        assertEquals(BetTitleCode.HOME_TEAM_SCORES, homeOver.code());
        assertFalse(homeOver.isNot());

        var awayUnder = OddsHalfLineSemanticMapper.mapTeamTotal(false, 0.5, OddsSelectionCode.UNDER).orElseThrow();
        assertEquals(BetTitleCode.AWAY_TEAM_SCORES, awayUnder.code());
        assertTrue(awayUnder.isNot());
    }
}
