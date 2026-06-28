package net.friendly_bets.wc26;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Kickoff UTC из даты и времени по Europe/Berlin (как на FIFA при country=DE). */
public final class Wc26BerlinKickoffCalculator {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private Wc26BerlinKickoffCalculator() {
    }

    public static LocalDateTime kickoffUtc(String date, String timeBerlin) {
        if (date == null || timeBerlin == null || date.isBlank() || timeBerlin.isBlank()) {
            return null;
        }
        LocalDate localDate = LocalDate.parse(date);
        LocalTime localTime = LocalTime.parse(timeBerlin);
        ZonedDateTime berlinKickoff = ZonedDateTime.of(localDate, localTime, BERLIN);
        return berlinKickoff.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    public static boolean isPlayoffStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return false;
        }
        return switch (stage) {
            case "round_of_32", "round_of_16", "quarter_final", "semi_final", "third_place", "final" -> true;
            default -> false;
        };
    }
}
