package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.gameresults.GameResultRecord;
import org.springframework.stereotype.Component;

/** Критерии включения матча / тура во внешний auto-poll. */
@Component
@RequiredArgsConstructor
public class GameResultPollingFilter {

    private final GameResultEffectiveKickoff effectiveKickoff;

    /** Матч уже идёт или пуск прошёл — нужен опрос до финализации. */
    public boolean needsExternalPoll(GameResultRecord record) {
        if (record == null || record.isFinalized()) {
            return false;
        }
        String status = MatchStatuses.normalize(record.getStatus());
        if (status != null && MatchStatuses.LIVE.contains(status)) {
            return true;
        }
        if (record.getLiveMinuteLabel() != null && !record.getLiveMinuteLabel().isBlank()) {
            return true;
        }
        return effectiveKickoff.isKickoffStarted(record);
    }
}
