package net.friendly_bets.services;

import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAliasResolverTest {

    @Mock
    TeamsRepository teamsRepository;

    TeamAliasResolver resolver = null;

    @Test
    @DisplayName("resolveFootballData matches by saved external alias name")
    void resolveFootballData_matchesByAliasName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByFootballDataTeamId(66)).thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasId("football-data", 66)).thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Manchester United FC"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Manchester United FC"))
                .thenReturn(Optional.of(Team.builder().id("mu1").title("ManchesterUnited").build()));

        Optional<Team> team = resolver.resolveFootballData(66, "Manchester United FC");

        assertTrue(team.isPresent());
        assertEquals("mu1", team.get().getId());
    }

    @Test
    @DisplayName("resolveFootballDataTeamId uses saved numeric id")
    void resolveFootballDataTeamId_usesSavedId() {
        resolver = new TeamAliasResolver(teamsRepository);
        Team brighton = Team.builder().id("b1").title("Brighton").footballDataTeamId(397).build();
        assertEquals(397, resolver.resolveFootballDataTeamId(brighton).orElse(-1));
    }
}
