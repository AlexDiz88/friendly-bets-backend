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

    public void updateAfterPollCycle(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null || record.isFinalized() || record.isAdminCorrected()) {
            return;
        }
        trackFirstTerminal(record, fetchedAt);

        var settings = settingsService.getEffective();
        if (!settings.isDualVerificationEnabled()) {
            updatePrimaryOnlyStability(record);
            return;
        }

        GameResultSourceSnapshot secondary = record.sourceFor(
                MatchDataProviders.sourcesStorageKey(settings.getSecondaryProvider()));
        if (!canIncrementDualStability(record, secondary, settings)) {
            resetDualStability(record, secondary);
            return;
        }

        String hash = combinedStabilityHash(record, secondary);
        if (hash != null && hash.equals(record.getLastSeenCombinedStabilityHash())) {
            int next = record.getStableScorePollCount() + 1;
            record.setStableScorePollCount(next);
            if (secondary != null) {
                secondary.setStableScorePollCount(next);
            }
        } else {
            record.setLastSeenCombinedStabilityHash(hash);
            int count = hash != null ? 1 : 0;
            record.setStableScorePollCount(count);
            if (secondary != null) {
                secondary.setStableScorePollCount(count);
                secondary.setLastSeenCanonicalScoreHash(canonicalScoreHash(secondary.getGameScore()));
            }
        }
        record.setLastSeenCanonicalScoreHash(canonicalScoreHash(record.getGameScore()));
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
        var settings = settingsService.getEffective();
        if (!settings.isDualVerificationEnabled()) {
            return true;
        }
        GameResultSourceSnapshot source = record.sourceFor(MatchDataProviders.sourcesStorageKey(secondaryProvider));
        if (source == null) {
            return false;
        }
        int required = settings.getRequireStablePolls();
        return source.getStableScorePollCount() >= required;
    }

    public String describeStabilityBlock(GameResultRecord record) {
        if (record == null) {
            return "record=null";
        }
        int required = settingsService.getEffective().getRequireStablePolls();
        String reason = "stablePolls=" + record.getStableScorePollCount() + "/" + required;
        var settings = settingsService.getEffective();
        if (settings.isDualVerificationEnabled()) {
            GameResultSourceSnapshot secondary = record.sourceFor(
                    MatchDataProviders.sourcesStorageKey(settings.getSecondaryProvider()));
            if (secondary != null) {
                reason += " secondaryStablePolls=" + secondary.getStableScorePollCount();
            }
        }
        return reason;
    }

    private void updatePrimaryOnlyStability(GameResultRecord record) {
        if (!MatchStatuses.isTerminal(MatchStatuses.normalize(record.getStatus()))) {
            record.setStableScorePollCount(0);
            return;
        }
        String hash = canonicalScoreHash(record.getGameScore());
        if (hash != null && hash.equals(record.getLastSeenCanonicalScoreHash())) {
            record.setStableScorePollCount(record.getStableScorePollCount() + 1);
        } else {
            record.setLastSeenCanonicalScoreHash(hash);
            record.setStableScorePollCount(hash != null ? 1 : 0);
        }
    }

    private static void trackFirstTerminal(GameResultRecord record, LocalDateTime fetchedAt) {
        String status = record.getStatus();
        if (status != null && MatchStatuses.isTerminal(status) && record.getFirstTerminalAt() == null) {
            record.setFirstTerminalAt(fetchedAt);
        }
    }

    private boolean canIncrementDualStability(
            GameResultRecord record,
            GameResultSourceSnapshot secondary,
            MatchResultSyncSettingsService.EffectiveMatchResultSyncSettings settings
    ) {
        if (!MatchStatuses.isTerminal(MatchStatuses.normalize(record.getStatus()))) {
            return false;
        }
        if (secondary == null || secondary.getGameScore() == null) {
            return false;
        }
        if (!MatchStatuses.isTerminal(MatchStatuses.normalize(secondary.getStatus()))) {
            return false;
        }
        return ProviderScoreComparator.matches(
                record.getGameScore(),
                secondary.getGameScore(),
                record.getScoreDuration(),
                secondary.getScoreDuration()
        );
    }

    private static void resetDualStability(GameResultRecord record, GameResultSourceSnapshot secondary) {
        record.setStableScorePollCount(0);
        record.setLastSeenCombinedStabilityHash(null);
        if (secondary != null) {
            secondary.setStableScorePollCount(0);
        }
    }

    private static String combinedStabilityHash(GameResultRecord record, GameResultSourceSnapshot secondary) {
        String primaryHash = canonicalScoreHash(record.getGameScore());
        if (primaryHash == null || secondary == null) {
            return null;
        }
        String secondaryHash = canonicalScoreHash(secondary.getGameScore());
        if (secondaryHash == null) {
            return null;
        }
        return primaryHash + "||" + secondaryHash;
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
