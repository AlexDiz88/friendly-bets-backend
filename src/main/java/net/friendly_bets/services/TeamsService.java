package net.friendly_bets.services;

import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;

public interface TeamsService {

    TeamsPage getAll();

    TeamsPage getLeagueTeams(String leagueId);

    TeamDto createTeam(NewTeamDto newTeam);

}
