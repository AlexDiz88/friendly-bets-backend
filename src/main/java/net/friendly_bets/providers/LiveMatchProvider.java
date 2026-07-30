package net.friendly_bets.providers;

import net.friendly_bets.models.Season;

import java.util.List;

/**
 * Layer LIVE: poll in-play status/minute/score for tracked matches.
 * One HTTP request per UTC kickoff date (all leagues on the same date page).
 */
public interface LiveMatchProvider extends ExternalDataProvider {

    LiveSyncResult syncLive(Season season);

    record LiveSyncResult(
            int httpRequests,
            int trackedCount,
            int updated,
            int finishedDetected,
            String message,
            List<String> datesSynced,
            List<String> pendingFullMatchIds
    ) {
        public LiveSyncResult {
            datesSynced = datesSynced == null ? List.of() : List.copyOf(datesSynced);
            pendingFullMatchIds = pendingFullMatchIds == null ? List.of() : List.copyOf(pendingFullMatchIds);
        }

        public static LiveSyncResult skipped(String message) {
            return new LiveSyncResult(0, 0, 0, 0, message, List.of(), List.of());
        }
    }
}
