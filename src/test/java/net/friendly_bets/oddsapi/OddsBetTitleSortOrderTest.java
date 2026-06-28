package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsBetTitleSortOrderTest {

    @Test
    void playoffRowsOrderAdvanceFirstHomeOrAwayLast() {
        List<OddsLineRow> rows = new ArrayList<>(List.of(
                row(BetTitleCode.PLAYOFF_HOME_OR_AWAY_REGULAR, false),
                row(BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE, false),
                row(BetTitleCode.PLAYOFF_EXTRA_TIME, false),
                row(BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE, false),
                row(BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES, true),
                row(BetTitleCode.PLAYOFF_HOME_WIN_REGULAR, false),
                row(BetTitleCode.PLAYOFF_HOME_OR_AWAY_OVERTIME, false)
        ));
        rows.sort(OddsBetTitleSortOrder.BY_PLAYOFF);

        assertEquals(BetTitleCode.PLAYOFF_HOME_ADVANCE_NEXT_STAGE, code(rows.get(0)));
        assertEquals(BetTitleCode.PLAYOFF_AWAY_ADVANCE_NEXT_STAGE, code(rows.get(1)));
        assertEquals(BetTitleCode.PLAYOFF_EXTRA_TIME, code(rows.get(2)));
        assertEquals(BetTitleCode.PLAYOFF_HOME_WIN_REGULAR, code(rows.get(3)));
        assertEquals(BetTitleCode.PLAYOFF_HOME_OR_AWAY_REGULAR, code(rows.get(4)));
        assertEquals(BetTitleCode.PLAYOFF_HOME_OR_AWAY_OVERTIME, code(rows.get(5)));
        assertEquals(BetTitleCode.PLAYOFF_HOME_OR_AWAY_PENALTIES, code(rows.get(6)));
    }

    @Test
    void cleanWinRowsOrderHomeThenAwayThenAnyYesBeforeNo() {
        List<OddsLineRow> rows = new ArrayList<>(List.of(
                row(BetTitleCode.CLEAN_WIN_AWAY, false),
                row(BetTitleCode.CLEAN_WIN_AWAY, true),
                row(BetTitleCode.CLEAN_WIN_ANY, false),
                row(BetTitleCode.CLEAN_WIN_HOME, true),
                row(BetTitleCode.CLEAN_WIN_HOME, false),
                row(BetTitleCode.CLEAN_WIN_ANY, true)
        ));
        rows.sort(OddsBetTitleSortOrder.BY_TEAM_SCOPE);

        assertEquals(BetTitleCode.CLEAN_WIN_HOME, code(rows.get(0)));
        assertEquals(false, rows.get(0).getBetTitle().isNot());
        assertEquals(BetTitleCode.CLEAN_WIN_HOME, code(rows.get(1)));
        assertEquals(true, rows.get(1).getBetTitle().isNot());
        assertEquals(BetTitleCode.CLEAN_WIN_AWAY, code(rows.get(2)));
        assertEquals(BetTitleCode.CLEAN_WIN_ANY, code(rows.get(4)));
    }

    private static OddsLineRow row(BetTitleCode code, boolean isNot) {
        return OddsLineRow.builder()
                .betTitle(BetTitle.builder()
                        .code(code.getCode())
                        .label(code.getLabel())
                        .isNot(isNot)
                        .build())
                .build();
    }

    private static BetTitleCode code(OddsLineRow row) {
        return BetTitleCode.fromCode(row.getBetTitle().getCode());
    }
}
