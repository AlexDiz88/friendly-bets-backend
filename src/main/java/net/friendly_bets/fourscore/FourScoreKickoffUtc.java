package net.friendly_bets.fourscore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 4score показывает дату/время в {@code Europe/Moscow}; в {@code game_results.utc_date} храним UTC.
 */
public final class FourScoreKickoffUtc {

    private static final ZoneId FOURSCORE_ZONE = ZoneId.of("Europe/Moscow");

    private FourScoreKickoffUtc() {
    }

    public static LocalDateTime fromMoscowLocal(LocalDateTime moscowLocal) {
        if (moscowLocal == null) {
            return null;
        }
        return moscowLocal.atZone(FOURSCORE_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    public static LocalDateTime fromMoscowLocal(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return fromMoscowLocal(date.atTime(time));
    }
}
