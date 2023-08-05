package net.friendly_bets.services;

import net.friendly_bets.dto.*;

import java.util.List;

public interface SeasonsService {

    SeasonsPage getAll();

    SeasonDto addSeason(NewSeasonDto newSeason);

    SeasonDto changeSeasonStatus(Long id, String status);

    List<String> getSeasonStatusList();

    SeasonDto getActiveSeason();

    SeasonDto getScheduledSeason();

    SeasonDto registrationInSeason(Long userId, Long seasonId);

    LeaguesPage getLeaguesBySeason(Long seasonId);

    SeasonDto addLeagueToSeason(Long seasonId, NewLeagueDto newLeague);

    SeasonDto addTeamToLeagueInSeason(Long seasonId, Long leagueId, Long teamId);

    SeasonDto addBetToLeagueInSeason(Long seasonId, Long leagueId, NewBetDto newBet);

    SeasonDto addEmptyBetToLeagueInSeason(Long seasonId, Long leagueId, NewEmptyBetDto newEmptyBet);

    SeasonDto addBetResult(Long seasonId, Long betId, NewBetResult newBetResult);
}
