package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsLineRowDeduperTest {

    @Test
    void mergesDuplicateHandicapRowsByBetTitle() {
        OddsLineRow first = row(BetTitleCode.HANDICAP_HOME_0, "1.31");
        OddsLineRow second = row(BetTitleCode.HANDICAP_HOME_0, "1.39");
        OddsLineRow third = row(BetTitleCode.HANDICAP_HOME_0, "1.38");

        List<OddsLineRow> deduped = OddsLineRowDeduper.dedupeRows(List.of(first, second, third));

        assertEquals(1, deduped.size());
        assertEquals("1.38", deduped.get(0).getBookmakerOdds().get("marathonbet"));
    }

    @Test
    void mergesDuplicateRowsByDisplayLabelWhenBetTitlesDiffer() {
        OddsLineRow a = row(BetTitleCode.HANDICAP_HOME_0, "1.31");
        a.setDisplayLabel("Ф1 (0)");
        OddsLineRow b = row(BetTitleCode.HANDICAP_HOME_PLUS_1_0, "1.39");
        b.setDisplayLabel("Ф1 (0)");

        List<OddsLineRow> deduped = OddsLineRowDeduper.dedupeRows(List.of(a, b));

        assertEquals(1, deduped.size());
        assertEquals("1.39", deduped.get(0).getBookmakerOdds().get("marathonbet"));
    }

    private static OddsLineRow row(BetTitleCode code, String odds) {
        return OddsLineRow.builder()
                .betTitle(BetTitle.builder()
                        .code(code.getCode())
                        .label(code.getLabel())
                        .isNot(false)
                        .build())
                .selectionCode("HOME")
                .line("0")
                .displayLabel(code.getLabel())
                .bookmakerOdds(new LinkedHashMap<>(java.util.Map.of("marathonbet", odds)))
                .build();
    }
}
