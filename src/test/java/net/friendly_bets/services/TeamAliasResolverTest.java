package net.friendly_bets.services;

import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
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
    @DisplayName("resolveByProviderName does not guess by display name")
    void resolveByProviderName_doesNotMatchDisplayNameVariant() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName(ExternalProviderIds.FLASHSCORE, "Arsenal"))
                .thenReturn(Optional.empty());

        Optional<Team> team = resolver.resolveByProviderName(ExternalProviderIds.FLASHSCORE, "Arsenal");

        assertTrue(team.isEmpty());
    }

    @Test
    @DisplayName("teamMatchesProviderSide matches only same-provider alias (exact name)")
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

    @Test
    @DisplayName("teamMatchesProviderSide does not match display name when alias locale differs")
    void teamMatchesProviderSide_doesNotMatchDisplayNameVariant() {
        TeamAliasResolver resolver = new TeamAliasResolver(teamsRepository);
        Team dortmund = Team.builder()
                .id("bvb1")
                .title("BorussiaDortmund")
                .displayNames(TeamDisplayNames.builder().en("Dortmund").ru("Дортмунд").build())
                .externalAliases(List.of(
                        net.friendly_bets.models.TeamExternalAlias.builder()
                                .provider(ExternalProviderIds.FLASHSCORE)
                                .externalName("Дортмунд")
                                .build()
                ))
                .build();

        assertFalse(resolver.teamMatchesProviderSide(dortmund, ExternalProviderIds.FLASHSCORE, "Dortmund"));
        assertTrue(resolver.teamMatchesProviderSide(dortmund, ExternalProviderIds.FLASHSCORE, "Дортмунд"));
    }
}
