package net.friendly_bets.gameresults;

import net.friendly_bets.footballdata.FootballDataMatchStatuses;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultFinalizedSource;
import net.friendly_bets.models.gameresults.GameResultRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Финализация канонического результата (без привязки к времени матча).
 * <ul>
 *   <li>терминальный {@code status} + валидный {@code fullTime}</li>
 *   <li>или валидный {@code fullTime} + {@code firstTime} (случай IN_PLAY с итоговым счётом от API)</li>
 * </ul>
 */
@Component
public class GameResultFinalizer {

    public void tryFinalize(GameResultRecord record, LocalDateTime now) {
        if (record == null || record.getFinalizedAt() != null || record.isAdminCorrected()) {
            return;
        }
        GameScore score = record.getGameScore();
        if (!GameScoreValidator.hasValidFullTime(score)) {
            return;
        }

        String status = FootballDataMatchStatuses.normalize(record.getStatus());
        boolean terminal = FootballDataMatchStatuses.isTerminal(status);
        boolean scoreWithHalftime = GameScoreValidator.hasValidFirstTime(score);

        if (!terminal && !scoreWithHalftime) {
            return;
        }

        if (!terminal) {
            record.setStatus("FINISHED");
        }
        record.setFinalizedAt(now);
        record.setFinalizedSource(GameResultFinalizedSource.API.name());
    }
}
