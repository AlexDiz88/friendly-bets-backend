package net.friendly_bets.fourscore;

import lombok.Builder;
import lombok.Value;
import net.friendly_bets.models.gameresults.GameResultRecord;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Value
@Builder
public class FourScoreListMatch {
    FourScoreLeagueSection section;
    String eventSlug;
    String eventPath;
    String homeTeamName;
    String awayTeamName;
    String statusText;
    Integer homeScore;
    Integer awayScore;
    LocalTime kickoffTime;
    Long externalEventId;

    public boolean isTerminal() {
        if (statusText == null) {
            return false;
        }
        String s = statusText.trim().toLowerCase();
        return s.contains("завершено");
    }

    public boolean needsEventDetails() {
        return FourScoreStatusTextParser.needsEventDetails(statusText, homeScore, awayScore);
    }

    /** Опрос 4score: пропускаем неначавшиеся и уже финализированные в БД. */
    public boolean shouldPollForRecord(GameResultRecord record) {
        if (record != null && record.isFinalized()) {
            return false;
        }
        if (needsEventDetails()) {
            return true;
        }
        return record != null && kickoffStarted(record);
    }

    private boolean kickoffStarted(GameResultRecord record) {
        LocalDateTime kickoff = record.getUtcDate();
        return kickoff != null && !LocalDateTime.now().isBefore(kickoff);
    }
}
