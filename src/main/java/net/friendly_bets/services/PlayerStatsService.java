package net.friendly_bets.services;

import net.friendly_bets.dto.*;

public interface PlayerStatsService {

    AllPlayersStatsDto getAllPlayersStatsBySeason(String seasonId);

    AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId);

}
