package net.friendly_bets.providers;

import net.friendly_bets.dto.ScheduleSyncResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

/**
 * Layer SCHEDULE: upsert matchday fixtures into {@code match_schedules}.
 * Default (cron / no matchday): current + next. Admin may pass a specific matchday order.
 */
public interface ScheduleProvider extends ExternalDataProvider {

    ScheduleSyncResultDto syncLeague(Season season, League league, boolean respectFarKickoffSkip);

    /**
     * @param matchday optional slot order; {@code null} → league current + next
     */
    ScheduleSyncResultDto syncByLeagueCode(String leagueCode, Integer matchday);
}
