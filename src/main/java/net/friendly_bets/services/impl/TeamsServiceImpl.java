package net.friendly_bets.services.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.TeamsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static net.friendly_bets.utils.GetEntityOrThrow.getLeagueOrThrow;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeamsServiceImpl implements TeamsService {

    TeamsRepository teamsRepository;
    LeaguesRepository leaguesRepository;

    @Override
    public TeamsPage getAll() {
        List<Team> allTeams = teamsRepository.findAll();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public TeamsPage getLeagueTeams(String leagueId) {
        League league = getLeagueOrThrow(leaguesRepository, leagueId);
        List<Team> allTeams = league.getTeams();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //


    @Override
    @Transactional
    public TeamDto createTeam(NewTeamDto newTeam) {
        if (teamsRepository.existsByTitle(newTeam.getTitle())) {
            throw new ConflictException("teamWithThisTitleAlreadyExist");
        }

        Team team = Team.builder()
                .createdAt(LocalDateTime.now())
                .title(newTeam.getTitle())
                .country(newTeam.getCountry())
                .logo("")
                .build();
        // TODO переделать инициализацию logo, после добавления функционала загрузки изображений с ПК

        teamsRepository.save(team);
        return TeamDto.from(team);
    }

    // ------------------------------------------------------------------------------------------------------ //
}
