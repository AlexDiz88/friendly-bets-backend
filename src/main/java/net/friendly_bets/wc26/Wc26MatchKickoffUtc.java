package net.friendly_bets.wc26;

import net.friendly_bets.models.gameresults.GameResultRecord;

import java.time.LocalDateTime;
import java.util.Optional;

/** Kickoff UTC из 4score ({@code utcDate}, MSK→UTC при синке). */
public final class Wc26MatchKickoffUtc {

    private Wc26MatchKickoffUtc() {
    }

    /** Для отображения и 4score — только {@code utcDate}. */
    public static Optional<LocalDateTime> resolve(GameResultRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(record.getUtcDate());
    }

    /**
     * Для сопоставления с Marathon/odds: при наличии {@code wc26ScheduleId} — kickoff из расписания стадиона,
     * иначе {@code utcDate} 4score.
     */
    public static Optional<LocalDateTime> resolveForEventMatching(GameResultRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        Integer scheduleId = record.getWc26ScheduleId();
        if (scheduleId != null) {
            Optional<LocalDateTime> fromSchedule = Wc26ScheduleKickoffLookup.kickoffUtc(scheduleId);
            if (fromSchedule.isPresent()) {
                return fromSchedule;
            }
        }
        return Optional.ofNullable(record.getUtcDate());
    }
}
