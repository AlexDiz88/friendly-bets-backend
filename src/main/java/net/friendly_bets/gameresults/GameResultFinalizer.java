package net.friendly_bets.gameresults;

import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.gameresults.MatchStatuses;
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

    public GameResultFinalizer(MatchResultTrustPolicy trustPolicy, ApiSyncIssueService apiSyncIssueService) {
        this.trustPolicy = trustPolicy;
        this.apiSyncIssueService = apiSyncIssueService;
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
        switch (decision) {
            case READY -> {
                record.setStatus(status);
                record.setFinalizedAt(now);
                record.setFinalizedSource(GameResultFinalizedSource.API.name());
            }
            case INVALID_SCORE -> apiSyncIssueService.recordInvalidCanonicalScore(record);
            case NOT_STABLE -> apiSyncIssueService.recordScoreNotStable(record);
            case PROVIDER_MISMATCH -> apiSyncIssueService.recordProviderScoreMismatch(record);
            case SECONDARY_UNAVAILABLE -> apiSyncIssueService.recordSecondaryProviderUnavailable(record);
            case PRIMARY_UNAVAILABLE -> apiSyncIssueService.recordPrimaryProviderUnavailable(record);
            default -> {
            }
        }
    }
}
