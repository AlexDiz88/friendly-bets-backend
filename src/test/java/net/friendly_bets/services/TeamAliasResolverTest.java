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

    @Test
    @DisplayName("resolveWc26Code matches by odds-api alias name from catalog")
    void resolveWc26Code_matchesByOddsApiName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Korea Republic"))
                .thenReturn(Optional.of(Team.builder().id("kor1").title("KoreaRepublic").build()));

        Optional<Team> team = resolver.resolveWc26Code("KOR");

        assertTrue(team.isPresent());
        assertEquals("kor1", team.get().getId());
    }

    @Test
    @DisplayName("resolveWc26Code falls back to football-data alias when odds-api alias is missing")
    void resolveWc26Code_fallsBackToFootballDataName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Canada"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Canada"))
                .thenReturn(Optional.of(Team.builder().id("can1").title("Canada").build()));

        Optional<Team> team = resolver.resolveWc26Code("CAN");

        assertTrue(team.isPresent());
        assertEquals("can1", team.get().getId());
    @Test
    @DisplayName("resolveWc26Code tries catalog alternates for odds-api name")
    void resolveWc26Code_triesOddsApiAlternates() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Czechia"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Czechia"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Czech Republic"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Czech Republic"))
                .thenReturn(Optional.of(Team.builder().id("cze1").title("CzechRepublic").build()));

        Optional<Team> team = resolver.resolveWc26Code("CZE");

        assertTrue(team.isPresent());
        assertEquals("cze1", team.get().getId());
    }

    @Test
    @DisplayName("resolveWc26Code returns empty when no alias matches")
    void resolveWc26Code_emptyWhenNoAliasMatches() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Korea Republic"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "Korea Republic"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("odds-api.io", "South Korea"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("football-data", "South Korea"))
                .thenReturn(Optional.empty());

        assertTrue(resolver.resolveWc26Code("KOR").isEmpty());
    }
}
