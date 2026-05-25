package net.friendly_bets.externaldata.footballdata;

import net.friendly_bets.models.ExpandedMatchdaySlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FootballDataBettingSlotApiMatchdayResolverTest {

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "6, 1",
            "7, 2",
            "12, 2",
            "13, 3",
            "18, 3",
    })
    void wcApiMatchdayFromOrder_mapsBettingSlotsToOfficialMatchdays(int order, int apiMatchday) {
        assertEquals(apiMatchday, FootballDataBettingSlotApiMatchdayResolver.wcApiMatchdayFromOrder(order));
    }

    @Test
    void resolveApiMatchday_wcGroupSlot_usesOrderNotId() {
        ExpandedMatchdaySlot slot = ExpandedMatchdaySlot.builder()
                .id("13")
                .order(13)
                .kind(ExpandedMatchdaySlot.Kind.GROUP)
                .labelKey("13")
                .build();
        assertEquals(3, FootballDataBettingSlotApiMatchdayResolver.resolveApiMatchday("WC", slot));
    }

    @Test
    void resolveApiMatchday_otherCompetition_usesSlotId() {
        ExpandedMatchdaySlot slot = ExpandedMatchdaySlot.builder()
                .id("7")
                .order(7)
                .kind(ExpandedMatchdaySlot.Kind.GROUP)
                .labelKey("7")
                .build();
        assertEquals(7, FootballDataBettingSlotApiMatchdayResolver.resolveApiMatchday("CL", slot));
    }

    @Test
    void wcApiMatchdayFromOrder_rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> FootballDataBettingSlotApiMatchdayResolver.wcApiMatchdayFromOrder(0));
        assertThrows(IllegalArgumentException.class,
                () -> FootballDataBettingSlotApiMatchdayResolver.wcApiMatchdayFromOrder(19));
    }
}
