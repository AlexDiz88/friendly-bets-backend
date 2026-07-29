package net.friendly_bets.matchschedule;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Cron-only optimization for SCHEDULE sync: skip HTTP when current is complete and far,
 * but keep polling while any other window matchday (e.g. next) still lacks {@code utc_kickoff}.
 */
@Component
@RequiredArgsConstructor
public class ScheduleFarKickoffSkipSupport {

    private final MatchScheduleRepository matchScheduleRepository;

    public boolean shouldSkip(
            String seasonId,
            String leagueId,
            int currentOrder,
            Set<Integer> window,
            int skipWhenKickoffFartherThanDays
    ) {
        if (skipWhenKickoffFartherThanDays <= 0 || window == null || window.isEmpty()) {
            return false;
        }

        List<MatchSchedule> currentRows = matchScheduleRepository
                .findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(leagueId, seasonId, currentOrder);
        if (currentRows == null || currentRows.isEmpty() || !allHaveUtcKickoff(currentRows)) {
            return false;
        }

        Instant earliestCurrent = earliestKickoff(currentRows);
        if (earliestCurrent == null) {
            return false;
        }
        long daysUntilCurrent = ChronoUnit.DAYS.between(Instant.now(), earliestCurrent);
        if (daysUntilCurrent <= skipWhenKickoffFartherThanDays) {
            return false;
        }

        for (Integer matchday : window) {
            if (matchday == null || matchday == currentOrder) {
                continue;
            }
            List<MatchSchedule> rows = matchScheduleRepository
                    .findByLeagueIdAndSeasonIdAndMatchdayOrderByUtcKickoffAsc(leagueId, seasonId, matchday);
            if (rows == null || rows.isEmpty() || !allHaveUtcKickoff(rows)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allHaveUtcKickoff(List<MatchSchedule> rows) {
        for (MatchSchedule row : rows) {
            if (row == null || row.getUtcKickoff() == null) {
                return false;
            }
        }
        return true;
    }

    private static Instant earliestKickoff(List<MatchSchedule> rows) {
        Instant earliest = null;
        for (MatchSchedule row : rows) {
            if (row != null && row.getUtcKickoff() != null) {
                if (earliest == null || row.getUtcKickoff().isBefore(earliest)) {
                    earliest = row.getUtcKickoff();
                }
            }
        }
        return earliest;
    }
}
