package net.friendly_bets.footballdata;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.GameResultFinalizer;
import net.friendly_bets.gameresults.MatchDataProviders;
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

    public boolean isLockedAgainstApiSync(GameResultRecord record) {
        return record != null && record.isAdminCorrected();
    }

    /**
     * Первая запись или provisional: полное обновление канона и снимка API, затем попытка финализации.
     */
    public void applyProvisionalSync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);
        existing.setStatus(incoming.getStatus());
        existing.setUtcDate(incoming.getUtcDate());
        existing.setGameScore(incoming.getGameScore());
        existing.setFetchedAt(fetchedAt);
        replaceFootballDataSource(existing, incoming.footballDataSource());
        gameResultFinalizer.tryFinalize(existing, fetchedAt);
    }

    /**
     * Уже финализировано API: канонический счёт не меняется; обновляются status/fetchedAt и снимок API.
     */
    public void applyFinalizedApiSync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        apiSyncIssueService.recordApiScoreChangedIfNeeded(existing, incoming);
        existing.setStatus(incoming.getStatus());
        existing.setFetchedAt(fetchedAt);
        mergeFinalizedFootballDataSource(existing, incoming.footballDataSource(), fetchedAt);
    }

    public void applySync(GameResultRecord existing, GameResultRecord incoming, LocalDateTime fetchedAt) {
        if (existing.getFinalizedAt() != null) {
            applyFinalizedApiSync(existing, incoming, fetchedAt);
        } else {
            applyProvisionalSync(existing, incoming, fetchedAt);
        }
    }

    private static void replaceFootballDataSource(GameResultRecord record, GameResultSourceSnapshot incoming) {
        if (incoming == null) {
            return;
        }
        Map<String, GameResultSourceSnapshot> sources = record.getSources();
        if (sources == null) {
            sources = new HashMap<>();
            record.setSources(sources);
        }
        sources.put(MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA), incoming);
    }

    private static void mergeFinalizedFootballDataSource(
            GameResultRecord record,
            GameResultSourceSnapshot incoming,
            LocalDateTime fetchedAt
    ) {
        if (incoming == null) {
            return;
        }
        Map<String, GameResultSourceSnapshot> sources = record.getSources();
        if (sources == null) {
            sources = new HashMap<>();
            record.setSources(sources);
        }
        String storageKey = MatchDataProviders.sourcesStorageKey(MatchDataProviders.FOOTBALL_DATA);
        GameResultSourceSnapshot stored = sources.get(storageKey);
        if (stored == null) {
            sources.put(storageKey, incoming);
            return;
        }
        stored.setStatus(incoming.getStatus());
        stored.setFetchedAt(fetchedAt);
        stored.setApiLastUpdated(incoming.getApiLastUpdated());
        stored.setGameScore(incoming.getGameScore());
    }
}
