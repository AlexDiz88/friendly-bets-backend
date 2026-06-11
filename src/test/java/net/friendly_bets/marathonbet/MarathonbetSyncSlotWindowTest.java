package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarathonbetSyncSlotWindowTest {

    @Test
    void resolvesCurrentAndNextSlot() {
        ExternalCompetitionInfoDto info = infoAtMatchday(3);

        assertEquals(List.of(3, 4), MarathonbetSyncSlotWindow.resolveSlotOrders(info));
        assertEquals(List.of(3), MarathonbetSyncSlotWindow.resolveSlotOrders(info, MarathonbetSlotScope.CURRENT));
        assertEquals(List.of(4), MarathonbetSyncSlotWindow.resolveSlotOrders(info, MarathonbetSlotScope.NEXT));
    }

    @Test
    void nextSlotEmptyOnFinalMatchday() {
        ExternalCompetitionInfoDto info = infoAtMatchday(4);

        assertEquals(List.of(4), MarathonbetSyncSlotWindow.resolveSlotOrders(info, MarathonbetSlotScope.CURRENT));
        assertEquals(List.of(), MarathonbetSyncSlotWindow.resolveSlotOrders(info, MarathonbetSlotScope.NEXT));
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
