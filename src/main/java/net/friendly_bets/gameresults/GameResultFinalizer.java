package net.friendly_bets.gameresults;

import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.gameresults.GameResultFinalizedSource;
import net.friendly_bets.models.gameresults.GameResultRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Финализация канонического результата после terminal status, валидации счёта,
 * стабилизации и (опционально) dual-API проверки.
 */
@Component
public class GameResultFinalizer {

    private final MatchResultTrustPolicy trustPolicy;
    private final ApiSyncIssueService apiSyncIssueService;
    private final MatchResultSyncSettingsService settingsService;

    public GameResultFinalizer(
            MatchResultTrustPolicy trustPolicy,
            ApiSyncIssueService apiSyncIssueService,
            MatchResultSyncSettingsService settingsService
    ) {
        this.trustPolicy = trustPolicy;
        this.apiSyncIssueService = apiSyncIssueService;
        this.settingsService = settingsService;
    }

    public void tryFinalize(GameResultRecord record, LocalDateTime now) {
        if (record == null || record.getFinalizedAt() != null || record.isAdminCorrected()) {
            return;
        }
        GameScore score = record.getGameScore();
        if (!GameScoreValidator.hasValidFullTime(score)) {
            return;
        }

        String status = MatchStatuses.normalize(record.getStatus());
        if (!MatchStatuses.isTerminal(status)) {
            return;
        }

        MatchResultTrustPolicy.FinalizeDecision decision = trustPolicy.evaluate(record, now);
        if (decision == MatchResultTrustPolicy.FinalizeDecision.TOO_EARLY) {
            return;
        }

        switch (decision) {
            case READY -> {
                record.setStatus(status);
                record.setFinalizedAt(now);
                record.setFinalizedSource(GameResultFinalizedSource.API.name());
            }
            case INVALID_SCORE -> recordIssueIfAllowed(record, () -> apiSyncIssueService.recordInvalidCanonicalScore(record));
            case NOT_STABLE -> recordIssueIfAllowed(record, () -> apiSyncIssueService.recordScoreNotStable(record));
            case PROVIDER_MISMATCH -> recordSecondaryIssueIfAllowed(
                    record,
                    () -> apiSyncIssueService.recordProviderScoreMismatch(record)
            );
            case SECONDARY_UNAVAILABLE -> recordSecondaryIssueIfAllowed(
                    record,
                    () -> apiSyncIssueService.recordSecondaryProviderUnavailable(record)
            );
            case PRIMARY_UNAVAILABLE -> recordIssueIfAllowed(
                    record,
                    () -> apiSyncIssueService.recordPrimaryProviderUnavailable(record)
            );
            default -> {
            }
        }
    }

    private void recordIssueIfAllowed(GameResultRecord record, Runnable action) {
        if (shouldRecordFinalizeIssue(record)) {
            action.run();
        }
    }

    private void recordSecondaryIssueIfAllowed(GameResultRecord record, Runnable action) {
        if (!settingsService.getEffective().isDualVerificationEnabled()) {
            return;
        }
        recordIssueIfAllowed(record, action);
    }

    private boolean shouldRecordFinalizeIssue(GameResultRecord record) {
        int required = settingsService.getEffective().getRequireStablePolls();
        return record.getPollCycleCount() >= required;
    }
}
