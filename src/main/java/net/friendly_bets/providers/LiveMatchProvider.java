package net.friendly_bets.providers;

import net.friendly_bets.models.Season;
import net.friendly_bets.providers.live.LiveMatchSupport;

import java.util.List;

/**
 * Layer LIVE: poll in-play status / minute / score for tracked matches.
 *
 * <p><b>Contract (all implementations):</b>
 * <ul>
 *   <li>One HTTP request per UTC kickoff <em>date</em> of tracked candidates
 *       (group schedules by {@code LocalDate.ofInstant(utcKickoff, UTC)}).</li>
 *   <li>Candidate selection / finished / wake windows — only via {@link LiveMatchSupport}
 *       (do not redefine per provider).</li>
 *   <li>Match resolve — aliases of <em>this</em> {@link #providerId()} only; never write
 *       {@code utc_kickoff}.</li>
 *   <li>Write {@code status}, {@code live_minute} / {@code live_minute_label}, {@code game_score},
 *       {@code fetchedAt}; return {@code pendingFullMatchIds} for FINISHED without FULL.</li>
 *   <li>Wake / poll interval — layer {@link net.friendly_bets.providers.live.LiveMatchWakeScheduler},
 *       not a provider-local cron.</li>
 * </ul>
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
