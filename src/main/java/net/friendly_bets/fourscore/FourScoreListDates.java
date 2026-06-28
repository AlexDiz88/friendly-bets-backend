package net.friendly_bets.fourscore;

import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.wc26.Wc26ScheduleKickoffLookup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Calendar dates for 4score {@code /events/?date=} — list pages use {@code Europe/Moscow}.
 */
public final class FourScoreListDates {

    private static final ZoneId LIST_ZONE = ZoneId.of("Europe/Moscow");

    private FourScoreListDates() {
    }

    public static LocalDate listPageDateFromKickoffUtc(LocalDateTime kickoffUtc) {
        if (kickoffUtc == null) {
            return LocalDate.now(LIST_ZONE);
        }
        return kickoffUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(LIST_ZONE).toLocalDate();
    }

    public static LocalDate listPageDateFromStoredUtc(LocalDateTime storedUtc) {
        return listPageDateFromKickoffUtc(storedUtc);
    }

    public static LocalDate todayInListZone() {
        return LocalDate.now(LIST_ZONE);
    }

    /** @deprecated use {@link net.friendly_bets.wc26.Wc26ScheduleKickoffResolver#listPageDatesForWcSlot} */
    @Deprecated
    public static Set<LocalDate> listPageDatesForWcSlot(String slotId) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            Wc26ScheduleKickoffLookup.kickoffUtc(scheduleId)
                    .ifPresent(kickoff -> dates.add(listPageDateFromKickoffUtc(kickoff)));
        }
        return dates;
    }
}
