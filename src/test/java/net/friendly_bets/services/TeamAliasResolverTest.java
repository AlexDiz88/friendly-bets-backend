package net.friendly_bets.services;

import net.friendly_bets.models.Team;
import net.friendly_bets.providers.ExternalProviderIds;
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
    @DisplayName("resolveByProviderName matches by saved external alias name")
    void resolveByProviderName_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName(ExternalProviderIds.TWENTYFOUR_SCORE, "Турция"))
                .thenReturn(Optional.of(Team.builder().id("tur1").title("Turkey").build()));

        Optional<Team> team = resolver.resolveByProviderName(ExternalProviderIds.TWENTYFOUR_SCORE, "Турция");

        assertTrue(team.isPresent());
        assertEquals("tur1", team.get().getId());
    }

    @Test
    @DisplayName("teamMatchesProviderSide matches only same-provider alias")
    void teamMatchesProviderSide_matchesByAliasName() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        Team england = Team.builder()
                .id("eng1")
                .title("England")
                .externalAliases(List.of(
                        net.friendly_bets.models.TeamExternalAlias.builder()
                                .provider(ExternalProviderIds.TWENTYFOUR_SCORE)
                                .externalName("Англия")
                                .build()
                ))
                .build();

        assertTrue(resolver.teamMatchesProviderSide(england, ExternalProviderIds.TWENTYFOUR_SCORE, "Англия"));
        assertFalse(resolver.teamMatchesProviderSide(england, ExternalProviderIds.TWENTYFOUR_SCORE, "Франция"));
        assertFalse(resolver.teamMatchesProviderSide(england, ExternalProviderIds.SOCCER365, "Англия"));
    }
}
