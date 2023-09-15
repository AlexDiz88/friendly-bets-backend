package net.friendly_bets.services;

import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsPage;

public interface PlayerStatsService {

    AllPlayersStatsPage getAllPlayersStatsBySeason(String seasonId);

    AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId);

    AllPlayersStatsPage playersStatsFullRecalculation(String seasonId);

}
