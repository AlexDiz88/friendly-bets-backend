package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.GameResultFinalizer;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultStabilizationService;
import net.friendly_bets.gameresults.MatchResultSyncSettingsService;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameResultPersistence {

    private final GameResultFinalizer gameResultFinalizer;
    private final ApiSyncIssueService apiSyncIssueService;
    private final MatchResultStabilizationService stabilizationService;
    private final MatchResultSyncSettingsService matchResultSyncSettingsService;

    public boolean isLockedAgainstApiSync(GameResultRecord record) {
        return record != null && record.isAdminCorrected();
    }

    public void applySync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        applyProviderSync(existing, incoming, MatchDataProviders.FOOTBALL_DATA, fetchedAt);
    }

    public void applyProviderSync(
            GameResultRecord existing,
            GameResultRecord incoming,
            String providerId,
            LocalDateTime fetchedAt
    ) {
        if (existing.getFinalizedAt() != null) {
            applyFinalizedProviderSync(existing, incoming, providerId, fetchedAt);
        } else {
            applyProvisionalProviderSync(existing, incoming, providerId, fetchedAt);
        }
    }

    /**
     * Первая запись или provisional: primary обновляет канон, secondary — только sources.
     */
    public void applyProvisionalProviderSync(
            GameResultRecord existing,
            GameResultRecord incoming,
            String providerId,
            LocalDateTime fetchedAt
    ) {
        if (isPrimaryProvider(providerId)) {
            apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);
            existing.setStatus(incoming.getStatus());
            if (incoming.getUtcDate() != null) {
                existing.setUtcDate(incoming.getUtcDate());
            }
            existing.setGameScore(incoming.getGameScore());
            existing.setScoreDuration(incoming.getScoreDuration());
            existing.setProvider(providerId);
            existing.setFetchedAt(fetchedAt);
            stabilizationService.updateStabilityCounters(existing, fetchedAt);
            gameResultFinalizer.tryFinalize(existing, fetchedAt);
        }
        mergeProviderSource(existing, incoming, providerId, fetchedAt, false);
        mergeSecondarySources(existing, incoming, providerId);
    }

    /**
     * Уже финализировано: канонический счёт не меняется; обновляются status/fetchedAt и снимок провайдера.
     */
    public void applyFinalizedProviderSync(
            GameResultRecord existing,
            GameResultRecord incoming,
            String providerId,
            LocalDateTime fetchedAt
    ) {
        if (isPrimaryProvider(providerId)) {
            apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);
            existing.setStatus(incoming.getStatus());
            existing.setFetchedAt(fetchedAt);
        }
        mergeProviderSource(existing, incoming, providerId, fetchedAt, true);
        mergeSecondarySources(existing, incoming, providerId);
    }

    /** @deprecated use {@link #applyProvisionalProviderSync} */
    @Deprecated
    public void applyProvisionalSync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        applyProvisionalProviderSync(existing, incoming, MatchDataProviders.FOOTBALL_DATA, fetchedAt);
    }

    /** @deprecated use {@link #applyFinalizedProviderSync} */
    @Deprecated
    public void applyFinalizedApiSync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        applyFinalizedProviderSync(existing, incoming, MatchDataProviders.FOOTBALL_DATA, fetchedAt);
    }

    private boolean isPrimaryProvider(String providerId) {
        String primary = matchResultSyncSettingsService.getEffective().getPrimaryProvider();
        return providerId != null && providerId.equals(primary);
    }

    private static void mergeSecondarySources(
            GameResultRecord existing,
            GameResultRecord incoming,
            String appliedProviderId
    ) {
        if (incoming == null || incoming.getSources() == null) {
            return;
        }
        Map<String, GameResultSourceSnapshot> sources = existing.getSources();
        if (sources == null) {
            sources = new HashMap<>();
            existing.setSources(sources);
        }
        for (Map.Entry<String, GameResultSourceSnapshot> entry : incoming.getSources().entrySet()) {
            String key = entry.getKey();
            String providerFromKey = key.replace('_', '-');
            if (providerFromKey.equals(appliedProviderId)) {
                continue;
            }
            if (MatchDataProviders.sourcesStorageKey(MatchDataProviders.API_FOOTBALL).equals(key)
                    || MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA).equals(key)
                    || MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOURSCORE).equals(key)) {
                sources.put(key, entry.getValue());
            }
        }
    }

    private static void mergeProviderSource(
            GameResultRecord record,
            GameResultRecord incoming,
            String providerId,
            LocalDateTime fetchedAt,
            boolean finalized
    ) {
        GameResultSourceSnapshot incomingSource = incomingSourceFor(incoming, providerId);
        if (incomingSource == null) {
            return;
        }
        Map<String, GameResultSourceSnapshot> sources = record.getSources();
        if (sources == null) {
            sources = new HashMap<>();
            record.setSources(sources);
        }
        String storageKey = MatchDataProviders.sourcesStorageKey(providerId);
        GameResultSourceSnapshot stored = sources.get(storageKey);
        if (stored == null) {
            sources.put(storageKey, incomingSource);
            return;
        }
        stored.setStatus(incomingSource.getStatus());
        stored.setFetchedAt(fetchedAt);
        stored.setApiLastUpdated(incomingSource.getApiLastUpdated());
        if (!finalized || stored.getGameScore() == null) {
            stored.setGameScore(incomingSource.getGameScore());
            stored.setScoreDuration(incomingSource.getScoreDuration());
        } else {
            stored.setGameScore(incomingSource.getGameScore());
            stored.setScoreDuration(incomingSource.getScoreDuration());
        }
    }

    private static GameResultSourceSnapshot incomingSourceFor(GameResultRecord incoming, String providerId) {
        if (incoming == null || incoming.getSources() == null) {
            return null;
        }
        return incoming.getSources().get(MatchDataProviders.sourcesStorageKey(providerId));
    }
}
