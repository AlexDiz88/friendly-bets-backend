package net.friendly_bets.wc26;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/** In-memory kickoff UTC по schedule_id — заполняется при старте из wc26_schedule. */
public final class Wc26ScheduleKickoffLookup {

    private static volatile Map<Integer, LocalDateTime> byScheduleId = Map.of();

    private Wc26ScheduleKickoffLookup() {
    }

    public static void install(Map<Integer, LocalDateTime> loaded) {
        byScheduleId = Map.copyOf(loaded);
    }

    public static Optional<LocalDateTime> kickoffUtc(int scheduleId) {
        return Optional.ofNullable(byScheduleId.get(scheduleId));
    }
}
