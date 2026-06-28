package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.wc26.Wc26ScheduleKickoffResolver;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Канонический kickoff UTC для game_results: для ЧМ26 — из wc26_schedule,
 * иначе utcDate записи (4score/24score).
 */
@Component
@RequiredArgsConstructor
public class GameResultEffectiveKickoff {

    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;

    public LocalDateTime resolve(GameResultRecord record) {
        if (record == null) {
            return null;
        }
        Integer scheduleId = record.getWc26ScheduleId();
        if (scheduleId != null) {
            return wc26ScheduleKickoffResolver.kickoffUtc(scheduleId).orElse(record.getUtcDate());
        }
        return record.getUtcDate();
    }

    public boolean isKickoffStarted(GameResultRecord record) {
        return isKickoffStarted(record, GameResultNotStarted.nowUtc());
    }

    public boolean isKickoffStarted(GameResultRecord record, LocalDateTime nowUtc) {
        LocalDateTime kickoff = resolve(record);
        return kickoff != null && !nowUtc.isBefore(kickoff);
    }
}
