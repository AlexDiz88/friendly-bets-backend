package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.enums.BetTitleCode;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsPeriodHandicapSubgroupSplitterTest {

    @Test
    void splitsFirstAndSecondHalfIntoNestedSubgroups() {
        List<OddsMarketGroup> groups = new ArrayList<>();
        groups.add(OddsMarketGroup.builder()
                .category(OddsMarketCategory.PERIOD_HANDICAP.name())
                .groupKey("periodHandicap")
                .rows(List.of(
                        row(BetTitleCode.FIRST_HALF_HANDICAP_HOME_0, "1.13"),
                        row(BetTitleCode.SECOND_HALF_HANDICAP_HOME_MINUS_2_0, "8.80")
                ))
                .build());

        OddsPeriodHandicapSubgroupSplitter.splitIntoSubgroups(groups);

        assertEquals(1, groups.size());
        OddsMarketGroup parent = groups.get(0);
        assertEquals("periodHandicap", parent.getGroupKey());
        assertTrue(parent.getRows() == null || parent.getRows().isEmpty());
        assertEquals(2, parent.getSubgroups().size());

        OddsMarketGroup firstHalf = parent.getSubgroups().get(0);
        assertEquals("firstHalfCorrectScore", firstHalf.getGroupKey());
        assertEquals(1, firstHalf.getRows().size());

        OddsMarketGroup secondHalf = parent.getSubgroups().get(1);
        assertEquals("secondHalfCorrectScore", secondHalf.getGroupKey());
        assertEquals(1, secondHalf.getRows().size());
        assertEquals("8.80", secondHalf.getRows().get(0).getBookmakerOdds().get("Marathonbet"));
    }

    private static OddsLineRow row(BetTitleCode code, String odds) {
        return OddsLineRow.builder()
                .line("-2")
                .selectionCode("HOME")
                .betTitle(BetTitle.builder()
                        .code(code.getCode())
                        .label(code.getLabel())
                        .isNot(false)
                        .build())
                .bookmakerOdds(java.util.Map.of("Marathonbet", odds))
                .build();
    }
}
