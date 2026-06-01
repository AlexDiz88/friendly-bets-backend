package net.friendly_bets.oddsapi;

import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.dto.ExternalMatchdaySlotDto;
import net.friendly_bets.models.League;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OddsApiSyncMatchdayWindowTest {

    @Test
    void wcCurrentPlusThreeNextSlots() {
        var info = ExternalCompetitionInfoDto.builder()
                .currentMatchday(5)
                .matchdaySlots(List.of(
                        slot(4), slot(5), slot(6), slot(7), slot(8)
                ))
                .build();

        assertEquals(
                List.of(5, 6, 7, 8),
                OddsApiSyncMatchdayWindow.resolveSlotOrders(info, League.LeagueCode.WC)
        );
        assertEquals(3, OddsApiSyncMatchdayWindow.additionalSlotsAfterCurrent(League.LeagueCode.WC));
    }

    @Test
    void leagueCurrentPlusOneNext() {
        var info = ExternalCompetitionInfoDto.builder()
                .currentMatchday(10)
                .matchdayCount(38)
                .build();

        assertEquals(
                List.of(10, 11),
                OddsApiSyncMatchdayWindow.resolveSlotOrders(info, League.LeagueCode.EPL)
        );
        assertEquals(1, OddsApiSyncMatchdayWindow.additionalSlotsAfterCurrent(League.LeagueCode.EPL));
    }

    @Test
    void clampsAtLastSlot() {
        var info = ExternalCompetitionInfoDto.builder()
                .currentMatchday(7)
                .matchdaySlots(List.of(slot(5), slot(6), slot(7)))
                .build();

        assertEquals(
                List.of(7),
                OddsApiSyncMatchdayWindow.resolveSlotOrders(info, League.LeagueCode.EC)
        );
    }

    private static ExternalMatchdaySlotDto slot(int value) {
        return ExternalMatchdaySlotDto.builder()
                .value(value)
                .slotId(String.valueOf(value))
                .label(String.valueOf(value))
                .kind("REGULAR")
                .build();
    }
}
