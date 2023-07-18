package net.friendly_bets.services;

import net.friendly_bets.dto.*;
import net.friendly_bets.security.details.AuthenticatedUser;

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

    LeagueDto addLeagueToSeason(String seasonId, NewLeagueDto newLeague);
}
