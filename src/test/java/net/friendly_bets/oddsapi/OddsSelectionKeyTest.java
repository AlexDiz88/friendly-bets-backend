package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OddsSelectionKeyTest {

    @Test
    void goalsRowsWithSameYesNoGetDistinctKeysWhenBetTitlesDiffer() {
        OddsLineRow bothScore = OddsLineRow.builder()
                .selectionCode("YES")
                .betTitle(BetTitle.builder().code((short) 1301).label("Обе забьют").isNot(false).build())
                .build();
        OddsLineRow homeScores = OddsLineRow.builder()
                .selectionCode("YES")
                .betTitle(BetTitle.builder().code((short) 1302).label("Хозяева забьют").isNot(false).build())
                .build();

        String key1 = OddsSelectionKey.build(OddsMarketCategory.GOALS, "goals", bothScore);
        String key2 = OddsSelectionKey.build(OddsMarketCategory.GOALS, "goals", homeScores);

        assertNotEquals(key1, key2);
    }
}
