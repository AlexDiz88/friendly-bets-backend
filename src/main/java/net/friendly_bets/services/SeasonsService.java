package net.friendly_bets.services;

import net.friendly_bets.dto.*;

import java.util.List;

public interface SeasonsService {

    SeasonsPage getAll();

    SeasonDto addSeason(NewSeasonDto newSeason);

    SeasonDto changeSeasonStatus(String id, String status);

    List<String> getSeasonStatusList();

    SeasonDto getActiveSeason();

    SeasonDto getScheduledSeason();

    SeasonDto registrationInSeason(String userId, String seasonId);

    LeaguesPage getLeaguesBySeason(String seasonId);

    SeasonDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague);

    SeasonDto addTeamToLeagueInSeason(String seasonId, String leagueId, String teamId);

    SeasonDto addBetToLeagueInSeason(String seasonId, String leagueId, NewBetDto newBet);

    SeasonDto addEmptyBetToLeagueInSeason(String seasonId, String leagueId, NewEmptyBetDto newEmptyBet);

    SeasonDto betResult(String seasonId, String betId, NewBetResult newBetResult);
}
