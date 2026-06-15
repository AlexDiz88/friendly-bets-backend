package net.friendly_bets.gameresults;

import net.friendly_bets.models.gameresults.GameResultRecord;

import java.time.LocalDateTime;

/** Критерии включения матча / тура во внешний auto-poll. */
public final class GameResultPollingFilter {

    private GameResultPollingFilter() {
    }

    /** Матч уже идёт или пуск прошёл — нужен опрос до финализации. */
    public static boolean needsExternalPoll(GameResultRecord record) {
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
        return kickoffStarted(record);
    }

    private static boolean kickoffStarted(GameResultRecord record) {
        LocalDateTime kickoff = record.getUtcDate();
        return kickoff != null && !LocalDateTime.now().isBefore(kickoff);
    }
}
