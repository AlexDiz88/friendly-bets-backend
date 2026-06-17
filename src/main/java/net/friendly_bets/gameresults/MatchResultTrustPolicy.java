package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MatchResultTrustPolicy {

    private final MatchResultSyncSettingsService settingsService;
    private final MatchResultStabilizationService stabilizationService;

    public enum FinalizeDecision {
        READY,
        NOT_STABLE,
        INVALID_SCORE,
        PROVIDER_MISMATCH,
        SECONDARY_UNAVAILABLE,
        PRIMARY_UNAVAILABLE,
        TOO_EARLY
    }

    public FinalizeDecision evaluate(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null || record.isAdminCorrected() || record.getFinalizedAt() != null) {
            return FinalizeDecision.NOT_STABLE;
        }
        if (!kickoffElapsed(record, fetchedAt)) {
            return FinalizeDecision.TOO_EARLY;
        }
        GameScore score = record.getGameScore();
        if (!GameScoreValidator.hasValidFullTime(score)
                || !GameScoreValidator.hasValidFirstTime(score)
                || !GameScoreConsistencyValidator.isConsistent(score)) {
            return FinalizeDecision.INVALID_SCORE;
        }
        if (!stabilizationService.isStableEnough(record, fetchedAt)) {
            return FinalizeDecision.NOT_STABLE;
        }

        var settings = settingsService.getEffective();
        if (!settings.isDualVerificationEnabled()) {
            return FinalizeDecision.READY;
        }

        String secondaryProvider = settings.getSecondaryProvider();
        GameScore primaryScore = resolveCanonicalForProvider(record, settings.getPrimaryProvider());
        GameResultSourceSnapshot secondarySource = record.sourceFor(
                MatchDataProviders.sourcesStorageKey(secondaryProvider));
        GameScore secondaryScore = secondarySource != null ? secondarySource.getGameScore() : null;

        if (primaryScore == null) {
            return FinalizeDecision.PRIMARY_UNAVAILABLE;
        }
        if (secondaryScore == null) {
            return settings.isAllowFinalizeWithoutSecondary()
                    ? FinalizeDecision.READY
                    : FinalizeDecision.SECONDARY_UNAVAILABLE;
        }
        if (!MatchStatuses.isTerminal(MatchStatuses.normalize(secondarySource.getStatus()))) {
            return FinalizeDecision.NOT_STABLE;
        }

        if (!stabilizationService.isSecondaryStableEnough(record, secondaryProvider)) {
            return FinalizeDecision.NOT_STABLE;
        }

        if (!ProviderScoreComparator.matches(
                primaryScore,
                secondaryScore,
                record.getScoreDuration(),
                secondarySource.getScoreDuration()
        )) {
            return FinalizeDecision.PROVIDER_MISMATCH;
        }
        return FinalizeDecision.READY;
    }

    private boolean kickoffElapsed(GameResultRecord record, LocalDateTime fetchedAt) {
        LocalDateTime kickoff = record.getUtcDate();
        if (kickoff == null || fetchedAt == null) {
            return true;
        }
        int minMinutes = settingsService.getEffective().getMinMinutesAfterKickoff();
        if (minMinutes <= 0) {
            return true;
        }
        return Duration.between(kickoff, fetchedAt).toMinutes() >= minMinutes;
    }

    private GameScore resolveCanonicalForProvider(GameResultRecord record, String providerId) {
        if (providerId == null || record == null) {
            return null;
        }
        String primary = settingsService.getEffective().getPrimaryProvider();
        if (providerId.equals(primary)) {
            return record.getGameScore();
        }
        GameResultSourceSnapshot source = record.sourceFor(MatchDataProviders.sourcesStorageKey(providerId));
        return source != null ? source.getGameScore() : null;
    }
}
