package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeamsService {

    TeamsRepository teamsRepository;
    GetEntityService getEntityService;

    public TeamsPage getAll() {
        List<Team> allTeams = teamsRepository.findAll();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    public TeamsPage getLeagueTeams(String leagueId) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        List<Team> allTeams = league.getTeams();
        return TeamsPage.builder()
                .teams(TeamDto.from(allTeams))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

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
