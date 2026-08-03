package net.friendly_bets.providers.odds;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsSlotWindowTest {

    @Test
    void resolvesCurrentAndNextSlot() {
        ExternalCompetitionInfoDto info = infoAtMatchday(3);

        assertEquals(List.of(3, 4), OddsSlotWindow.resolveSlotOrders(info));
        assertEquals(List.of(3), OddsSlotWindow.resolveSlotOrders(info, OddsSlotScope.CURRENT));
        assertEquals(List.of(4), OddsSlotWindow.resolveSlotOrders(info, OddsSlotScope.NEXT));
    }

    @Test
    void nextSlotEmptyOnFinalMatchday() {
        ExternalCompetitionInfoDto info = infoAtMatchday(4);

        assertEquals(List.of(4), OddsSlotWindow.resolveSlotOrders(info, OddsSlotScope.CURRENT));
        assertEquals(List.of(), OddsSlotWindow.resolveSlotOrders(info, OddsSlotScope.NEXT));
    }

    private static ExternalCompetitionInfoDto infoAtMatchday(int current) {
        return ExternalCompetitionInfoDto.builder()
                .currentMatchday(current)
                .matchdayCount(16)
                .matchdaySlots(List.of(
                        slot(1), slot(2), slot(3), slot(4)
                ))
                .build();
    }

    private static ExternalMatchdaySlotDto slot(int value) {
        return ExternalMatchdaySlotDto.builder().value(value).label("MD" + value).build();
    }
}
