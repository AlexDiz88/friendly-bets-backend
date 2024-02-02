package net.friendly_bets.services;

import net.friendly_bets.dto.AllPlayersStatsByLeaguesDto;
import net.friendly_bets.dto.AllPlayersStatsPage;
import net.friendly_bets.dto.AllStatsByTeamsInSeasonDto;

public interface PlayerStatsService {

    AllPlayersStatsPage getAllPlayersStatsBySeason(String seasonId);

    AllPlayersStatsByLeaguesDto getAllPlayersStatsByLeagues(String seasonId);

    AllStatsByTeamsInSeasonDto getAllStatsByTeamsInSeason(String seasonId);

    AllPlayersStatsPage playersStatsFullRecalculation(String seasonId);

    void playersStatsByTeamsRecalculation(String seasonId);

}
