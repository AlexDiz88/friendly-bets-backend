package net.friendly_bets.providers;

import net.friendly_bets.dto.Soccer365ScheduleSyncResultDto;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;

/**
 * Layer SCHEDULE: upsert current+next matchday fixtures into {@code match_schedules}.
 */
public interface ScheduleProvider extends ExternalDataProvider {

    Soccer365ScheduleSyncResultDto syncLeague(Season season, League league, boolean respectFarKickoffSkip);

    Soccer365ScheduleSyncResultDto syncByLeagueCode(String leagueCode);
}
