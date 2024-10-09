package net.friendly_bets.services;

import net.friendly_bets.dto.NewTeamDto;
import net.friendly_bets.dto.TeamDto;
import net.friendly_bets.dto.TeamsPage;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamsServiceTest {
    @Mock
    TeamsRepository teamsRepository;
    @Mock
    GetEntityService getEntityService;
    @InjectMocks
    TeamsService teamsService;

    Team team1;
    Team team2;
    NewTeamDto newTeamDto;
    String leagueId;
    League league;

    @BeforeEach
    void setUp() {
        team1 = Team.builder().title("Team A").build();
        team2 = Team.builder().title("Team B").build();
        leagueId = "leagueId";

        league = League.builder()
                .id(leagueId)
                .teams(Arrays.asList(team1, team2))
                .build();

        newTeamDto = NewTeamDto.builder()
                .title("Team A")
                .country("Country A")
                .build();
    }

    @Test
    @DisplayName("Should return TeamsPage with correct teams")
    void getAll_ReturnsValidTeamsPage_WhenTeamsExist() {
        // given
        List<Team> allTeams = Arrays.asList(team1, team2);
        when(teamsRepository.findAll()).thenReturn(allTeams);

        // when
        TeamsPage result = teamsService.getAll();

        // then
        assertNotNull(result);
        assertNotNull(result.getTeams());
        assertEquals(2, result.getTeams().size(), "Expected 2 teams in result TeamsPage");
        assertEquals(team1.getTitle(), result.getTeams().get(0).getTitle());
        assertEquals(team2.getTitle(), result.getTeams().get(1).getTitle());
        verify(teamsRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty TeamsPage when no teams are found")
    void getAll_ReturnsEmptyTeamsPage_WhenNoTeams() {
        // given
        List<Team> emptyTeams = Collections.emptyList();
        when(teamsRepository.findAll()).thenReturn(emptyTeams);

        // when
        TeamsPage result = teamsService.getAll();

        // then
        assertNotNull(result);
        assertEquals(0, result.getTeams().size(), "Expected 0 teams in result TeamsPage");
        verify(teamsRepository, times(1)).findAll();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should return TeamsPage with correct teams by league")
    void getLeagueTeams_ReturnsValidTeamsPage_WhenLeagueHasTeams() {
        // given
        when(getEntityService.getLeagueOrThrow(leagueId)).thenReturn(league);

        // when
        TeamsPage result = teamsService.getLeagueTeams(leagueId);

        // then
        assertNotNull(result);
        assertNotNull(result.getTeams());
        assertEquals(2, result.getTeams().size(), "Expected 2 teams in result TeamsPage");
        assertEquals(team1.getTitle(), result.getTeams().get(0).getTitle());
        assertEquals(team2.getTitle(), result.getTeams().get(1).getTitle());
        verify(getEntityService, times(1)).getLeagueOrThrow(leagueId);
    }

    @Test
    @DisplayName("Should return empty TeamsPage when league has no teams")
    void getLeagueTeams_ReturnsEmptyTeamsPage_WhenLeagueHasNoTeams() {
        // given
        League emptyLeague = League.builder()
                .id(leagueId)
                .teams(Collections.emptyList())
                .build();

        when(getEntityService.getLeagueOrThrow(leagueId)).thenReturn(emptyLeague);

        // when
        TeamsPage result = teamsService.getLeagueTeams(leagueId);

        // then
        assertNotNull(result);
        assertEquals(0, result.getTeams().size(), "Expected 0 teams in result TeamsPage");
        verify(getEntityService, times(1)).getLeagueOrThrow(leagueId);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Test
    @DisplayName("Should create new team when title is unique")
    void createTeam_ShouldCreateNewTeam_WhenTitleIsUnique() {
        // given
        when(teamsRepository.existsByTitle("Team A")).thenReturn(false);

        // when
        TeamDto result = teamsService.createTeam(newTeamDto);

        // then
        assertNotNull(result);
        assertEquals("Team A", result.getTitle());
        verify(teamsRepository, times(1)).save(argThat(team ->
                team.getTitle().equals("Team A") && team.getCountry().equals("Country A")
        ));
    }

    @Test
    @DisplayName("Should throw ConflictException when team title already exists")
    void createTeam_ShouldThrowConflictException_WhenTitleAlreadyExists() {
        // given
        when(teamsRepository.existsByTitle("Team A")).thenReturn(true);

        // when + then
        assertThrows(ConflictException.class, () -> teamsService.createTeam(newTeamDto),
                "Expected ConflictException due to existing team title");
        verify(teamsRepository, never()).save(any(Team.class));
    }
}
