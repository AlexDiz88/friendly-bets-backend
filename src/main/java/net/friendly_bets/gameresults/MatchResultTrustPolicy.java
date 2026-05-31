package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

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
        PRIMARY_UNAVAILABLE
    }

    public FinalizeDecision evaluate(GameResultRecord record, LocalDateTime fetchedAt) {
        if (record == null || record.isAdminCorrected() || record.getFinalizedAt() != null) {
            return FinalizeDecision.NOT_STABLE;
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

        GameScore primaryScore = resolveCanonicalForProvider(record, settings.getPrimaryProvider());
        GameScore secondaryScore = resolveCanonicalForProvider(record, settings.getSecondaryProvider());

        if (primaryScore == null) {
            return FinalizeDecision.PRIMARY_UNAVAILABLE;
        }
        if (secondaryScore == null) {
            return settings.isAllowFinalizeWithoutSecondary()
                    ? FinalizeDecision.READY
                    : FinalizeDecision.SECONDARY_UNAVAILABLE;
        }

        if (!GameScoreValidator.sameCanonicalScore(primaryScore, secondaryScore)) {
            return FinalizeDecision.PROVIDER_MISMATCH;
        }
        return FinalizeDecision.READY;
    }

    private static GameScore resolveCanonicalForProvider(GameResultRecord record, String providerId) {
        if (providerId == null) {
            return null;
        }
        if (MatchDataProviders.FOOTBALL_DATA.equals(providerId)) {
            return record.getGameScore();
        }
        GameResultSourceSnapshot source = record.sourceFor(MatchDataProviders.sourcesStorageKey(providerId));
        return source != null ? source.getGameScore() : null;
    }

    private static GameResultSourceSnapshot sourceForProvider(GameResultRecord record, String providerId) {
        if (providerId == null) {
            return null;
        }
        return record.sourceFor(MatchDataProviders.sourcesStorageKey(providerId));
    }
}
