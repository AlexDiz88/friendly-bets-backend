package net.friendly_bets.services;

import net.friendly_bets.dto.*;

import java.util.List;

public interface SeasonsService {

    SeasonsPage getAll();

    SeasonDto addSeason(NewSeasonDto newSeason);

    SeasonDto changeSeasonStatus(String id, String status);

    List<String> getSeasonStatusList();

    SeasonDto getActiveSeason();

    ActiveSeasonIdDto getActiveSeasonId();

    SeasonDto getScheduledSeason();

    SeasonDto registrationInSeason(String userId, String seasonId);

    LeaguesPage getLeaguesBySeason(String seasonId);

    SeasonDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague);

    LeagueDto addTeamToLeagueInSeason(String seasonId, String leagueId, String teamId);

    BetDto addBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewBetDto newBet);

    BetDto addEmptyBetToLeagueInSeason(String moderatorId, String seasonId, String leagueId, NewEmptyBetDto newEmptyBet);

    BetDto addBetResult(String moderatorId, String seasonId, String betId, NewBetResult newBetResult);
}
