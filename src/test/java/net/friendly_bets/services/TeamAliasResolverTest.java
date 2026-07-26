package net.friendly_bets.services;

import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.repositories.TeamsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAliasResolverTest {

    @Mock
    TeamsRepository teamsRepository;

    @Test
    @DisplayName("resolveTwentyFourScoreByName matches by saved external alias name")
    void resolveTwentyFourScoreByName_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("24score.pro", "Турция"))
                .thenReturn(Optional.of(Team.builder().id("tur1").title("Turkey").build()));

        Optional<Team> team = resolver.resolveTwentyFourScoreByName("Турция");

        assertTrue(team.isPresent());
        assertEquals("tur1", team.get().getId());
    }

    @Test
    @DisplayName("resolveMarathonbetByName matches by saved external alias name")
    void resolveMarathonbetByName_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("marathonbet", "Бельгия"))
                .thenReturn(Optional.of(Team.builder().id("bel1").title("Belgium").build()));

        Optional<Team> team = resolver.resolveMarathonbetByName("Бельгия");

        assertTrue(team.isPresent());
        assertEquals("bel1", team.get().getId());
    }

    @Test
    @DisplayName("resolveSoccer365ByName matches by saved external alias name")
    void resolveSoccer365ByName_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("soccer365.ru", "Арсенал"))
                .thenReturn(Optional.of(Team.builder().id("ars1").title("Arsenal").build()));

        Optional<Team> team = resolver.resolveSoccer365ByName("Арсенал");

        assertTrue(team.isPresent());
        assertEquals("ars1", team.get().getId());
    }

    @Test
    @DisplayName("teamMatchesScoreProviderSide matches only same-provider alias")
    void teamMatchesScoreProviderSide_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        Team england = Team.builder()
                .id("eng1")
                .title("England")
                .externalAliases(List.of(
                        net.friendly_bets.models.TeamExternalAlias.builder()
                                .provider(MatchDataProviders.TWENTYFOUR_SCORE)
                                .externalName("Англия")
                                .build()
                ))
                .build();

        assertTrue(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.TWENTYFOUR_SCORE, "Англия"));
        assertFalse(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.TWENTYFOUR_SCORE, "Франция"));
        assertFalse(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.SOCCER365, "Англия"));
    }
}
