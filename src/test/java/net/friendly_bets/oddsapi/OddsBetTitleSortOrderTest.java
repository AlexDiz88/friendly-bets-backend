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
