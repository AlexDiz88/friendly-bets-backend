package net.friendly_bets.wc26;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class Wc26KickoffUtcCalculator {

    private Wc26KickoffUtcCalculator() {
    }

    public static LocalDateTime kickoffUtc(String date, String timeLocal, String venueKey) {
        if (date == null || timeLocal == null) {
            return null;
        }
        LocalDate localDate = LocalDate.parse(date);
        LocalTime localTime = LocalTime.parse(timeLocal);
        ZoneId venueZone = ZoneId.of(Wc26VenueTimezones.forVenueKey(venueKey));
        ZonedDateTime venueKickoff = ZonedDateTime.of(localDate, localTime, venueZone);
        return venueKickoff.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }
}
