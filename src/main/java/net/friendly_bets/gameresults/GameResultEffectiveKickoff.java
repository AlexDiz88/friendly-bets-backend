package net.friendly_bets.gameresults;

import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Kickoff UTC for game_results: stored {@code utcDate} only. */
@Component
public class GameResultEffectiveKickoff {

    public LocalDateTime resolve(GameResultRecord record) {
        if (record == null) {
            return null;
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
