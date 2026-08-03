package net.friendly_bets.providers.odds;

import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.services.MatchScheduleDisplayService;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared ODDS refresh window check (layer-level, provider-agnostic).
 */
public final class OddsRefreshSupport {

    private OddsRefreshSupport() {
    }

    /**
     * Whether a not-started match should be fetched under {@link OddsFetchPolicy#REFRESH_WINDOW}.
     * Missing odds → always; existing odds → only if kickoff within {@code refreshWithinHours}.
     */
    public static boolean needsRefresh(
            MatchSchedule match,
            boolean hasOdds,
            Instant now,
            int refreshWithinHours
    ) {
        if (match == null || match.getId() == null) {
            return false;
        }
        if (MatchScheduleDisplayService.isFinalized(match)) {
            return false;
        }
        if (!MatchScheduleNotStarted.isNotStarted(match, now)) {
            return false;
        }
        if (match.getUtcKickoff() == null) {
            return false;
        }
        if (!hasOdds) {
            return true;
        }
        Instant kickoff = match.getUtcKickoff();
        if (refreshWithinHours <= 0) {
            return true;
        }
        Duration untilKickoff = Duration.between(now, kickoff);
        return !untilKickoff.isNegative()
                && untilKickoff.compareTo(Duration.ofHours(refreshWithinHours)) <= 0;
    }
}
