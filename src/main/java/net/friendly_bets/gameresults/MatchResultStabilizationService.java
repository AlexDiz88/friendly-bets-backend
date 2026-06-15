package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MatchResultStabilizationService {

    private final MatchResultSyncSettingsService settingsService;

    public void updateStabilityCounters(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null || record.isFinalized() || record.isAdminCorrected()) {
            return;
        }
        String status = record.getStatus();
        if (status != null && net.friendly_bets.gameresults.MatchStatuses.isTerminal(status)) {
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

    public void updateSecondaryStabilityCounters(GameResultSourceSnapshot source) {
        if (source == null) {
            return;
        }
        String hash = canonicalScoreHash(source.getGameScore());
        if (hash != null && hash.equals(source.getLastSeenCanonicalScoreHash())) {
            source.setStableScorePollCount(source.getStableScorePollCount() + 1);
        } else {
            source.setLastSeenCanonicalScoreHash(hash);
            source.setStableScorePollCount(hash != null ? 1 : 0);
        }
    }

    public boolean isStableEnough(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null) {
            return false;
        }
        var settings = settingsService.getEffective();
        return record.getStableScorePollCount() >= settings.getRequireStablePolls();
    }

    public boolean isSecondaryStableEnough(GameResultRecord record, String secondaryProvider) {
        if (record == null || secondaryProvider == null) {
            return false;
        }
        GameResultSourceSnapshot source = record.sourceFor(MatchDataProviders.sourcesStorageKey(secondaryProvider));
        if (source == null) {
            return false;
        }
        int required = settingsService.getEffective().getRequireStablePolls();
        return source.getStableScorePollCount() >= required;
    }

    public String describeStabilityBlock(GameResultRecord record) {
        if (record == null) {
            return "record=null";
        }
        int required = settingsService.getEffective().getRequireStablePolls();
        return "stablePolls=" + record.getStableScorePollCount() + "/" + required;
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
