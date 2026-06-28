package net.friendly_bets.fourscore;

import net.friendly_bets.wc26.Wc26KickoffUtcCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FourScoreListDatesTest {

    @Test
    void listPageDateFromKickoffUtc_fifaBerlinLateNightIsNextMoscowDay() {
        // FIFA DE 2026-07-01 03:00 → 2026-07-01 01:00 UTC → 2026-07-01 04:00 MSK
        LocalDateTime kickoffUtc = Wc26KickoffUtcCalculator.kickoffUtc(
                "2026-07-01", "03:00", "ignored");
        assertEquals(LocalDate.of(2026, 7, 1), FourScoreListDates.listPageDateFromKickoffUtc(kickoffUtc));
    }

    @Test
    void listPageDateFromKickoffUtc_fifaBerlinEveningIsSameMoscowDay() {
        // FIFA DE 2026-06-30 21:00 → 2026-06-30 19:00 UTC → 2026-06-30 22:00 MSK
        LocalDateTime kickoffUtc = Wc26KickoffUtcCalculator.kickoffUtc(
                "2026-06-30", "21:00", "ignored");
        assertEquals(LocalDate.of(2026, 6, 30), FourScoreListDates.listPageDateFromKickoffUtc(kickoffUtc));
    }
}
