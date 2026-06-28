package net.friendly_bets.fourscore;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FourScoreKickoffUtcTest {

    @Test
    void fromMoscowLocal_convertsToUtc() {
        LocalDateTime moscow = LocalDateTime.of(2026, 6, 29, 19, 0);
        LocalDateTime utc = FourScoreKickoffUtc.fromMoscowLocal(moscow);
        assertEquals(LocalDateTime.of(2026, 6, 29, 16, 0), utc);
    }

    @Test
    void fromMoscowLocal_dateAndTime_convertsToUtc() {
        LocalDateTime utc = FourScoreKickoffUtc.fromMoscowLocal(
                LocalDate.of(2026, 6, 29),
                LocalTime.of(22, 30)
        );
        assertEquals(LocalDateTime.of(2026, 6, 29, 19, 30), utc);
    }

    @Test
    void fromMoscowLocal_summerOffset_isUtcPlus3() {
        LocalDateTime utc = FourScoreKickoffUtc.fromMoscowLocal(LocalDateTime.of(2026, 6, 28, 21, 0));
        assertEquals(
                LocalDateTime.of(2026, 6, 28, 18, 0),
                utc.atZone(ZoneOffset.UTC).toLocalDateTime()
        );
    }
}
