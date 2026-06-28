package net.friendly_bets.wc26;

import java.time.LocalDateTime;

public final class Wc26KickoffUtcCalculator {

    private Wc26KickoffUtcCalculator() {
    }

    public static LocalDateTime kickoffUtc(String date, String timeLocal, String venueKey) {
        return kickoffUtc(date, timeLocal, venueKey, null);
    }

    public static LocalDateTime kickoffUtc(String date, String timeLocal, String venueKey, String stage) {
        if (date == null || timeLocal == null) {
            return null;
        }
        // timeLocal в wc26_schedule — FIFA country=DE (Europe/Berlin), не локаль стадиона
        return Wc26BerlinKickoffCalculator.kickoffUtc(date, timeLocal);
    }
}
