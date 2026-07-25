package net.friendly_bets.providers;

import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

import java.util.List;

/**
 * Layer LIVE: poll in-play status/minute/score for a league (typically one HTTP request per league/date).
 */
public interface LiveMatchProvider extends ExternalDataProvider {

    LiveSyncResult syncLeagueLive(Season season, League league);

    record LiveSyncResult(
            String leagueCode,
            int updated,
            int finishedDetected,
            String message,
            List<String> pendingFullMatchIds
    ) {
        public LiveSyncResult {
            pendingFullMatchIds = pendingFullMatchIds == null ? List.of() : List.copyOf(pendingFullMatchIds);
        }

        public static LiveSyncResult of(String leagueCode, int updated, int finishedDetected, String message) {
            return new LiveSyncResult(leagueCode, updated, finishedDetected, message, List.of());
        }
    }
}
