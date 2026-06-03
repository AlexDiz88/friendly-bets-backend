package net.friendly_bets.marathonbet;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarathonbetSyncSlotWindowTest {

    @Test
    void resolvesCurrentAndNextSlot() {
        ExternalCompetitionInfoDto info = ExternalCompetitionInfoDto.builder()
                .currentMatchday(3)
                .matchdayCount(16)
                .matchdaySlots(List.of(
                        slot(1), slot(2), slot(3), slot(4)
                ))
                .build();

        assertEquals(List.of(3, 4), MarathonbetSyncSlotWindow.resolveSlotOrders(info));
    }

    private static ExternalMatchdaySlotDto slot(int value) {
        return ExternalMatchdaySlotDto.builder().value(value).label("MD" + value).build();
    }
}
