package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.footballdata.FootballDataScoreNormalizer;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MatchResultStabilizationService {

    private final MatchResultSyncSettingsService settingsService;

    public void updateStabilityCounters(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null || record.isFinalized() || record.isAdminCorrected()) {
            return;
        }
        String status = record.getStatus();
        if (status != null && net.friendly_bets.footballdata.FootballDataMatchStatuses.isTerminal(status)) {
            if (record.getFirstTerminalAt() == null) {
                record.setFirstTerminalAt(fetchedAt);
            }
        }

        String hash = canonicalScoreHash(record.getGameScore());
        if (hash != null && hash.equals(record.getLastSeenCanonicalScoreHash())) {
            record.setStableScorePollCount(record.getStableScorePollCount() + 1);
        } else {
            record.setLastSeenCanonicalScoreHash(hash);
            record.setStableScorePollCount(hash != null ? 1 : 0);
        }
    }

    public boolean isStableEnough(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null) {
            return false;
        }
        var settings = settingsService.getEffective();
        if (record.getStableScorePollCount() < settings.getRequireStablePolls()) {
            return false;
        }
        if (record.getUtcDate() == null) {
            return false;
        }
        int minMinutes = isKnockout(record)
                ? settings.getMinMinutesAfterKickoffKnockout()
                : settings.getMinMinutesAfterKickoff();
        long minutesSinceKickoff = ChronoUnit.MINUTES.between(record.getUtcDate(), fetchedAt);
        if (minutesSinceKickoff < minMinutes) {
            return false;
        }
        return isApiLastUpdatedStaleEnough(record, fetchedAt, settings.getMinMinutesSinceApiLastUpdated());
    }

    private static boolean isKnockout(GameResultRecord record) {
        String duration = record.getScoreDuration();
        if (duration == null || duration.isBlank()) {
            return false;
        }
        return FootballDataScoreNormalizer.DURATION_EXTRA_TIME.equals(duration)
                || FootballDataScoreNormalizer.DURATION_PENALTY_SHOOTOUT.equals(duration);
    }

    private static boolean isApiLastUpdatedStaleEnough(
            GameResultRecord record,
            LocalDateTime fetchedAt,
            int minMinutesSinceApiLastUpdated
    ) {
        if (minMinutesSinceApiLastUpdated <= 0) {
            return true;
        }
        GameResultSourceSnapshot source = record.footballDataSource();
        if (source == null || source.getApiLastUpdated() == null) {
            return true;
        }
        long minutes = ChronoUnit.MINUTES.between(source.getApiLastUpdated(), fetchedAt);
        return minutes >= minMinutesSinceApiLastUpdated;
    }

    static String canonicalScoreHash(GameScore score) {
        if (!GameScoreValidator.hasValidFullTime(score)) {
            return null;
        }
        return String.join("|",
                normalize(score.getFullTime()),
                normalize(score.getFirstTime()),
                normalize(score.getOverTime()),
                normalize(score.getPenalty()));
    }

    private static String normalize(String part) {
        return part == null ? "" : part.trim();
    }
}
