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

    TeamAliasResolver resolver = null;

    @Test
    @DisplayName("resolveOddsApi prefers id over name when both match different teams")
    void resolveOddsApi_prefersIdOverName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasId("odds-api.io", 100))
                .thenReturn(Optional.of(Team.builder().id("by-id").title("ById").build()));
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Other Name"))
                .thenReturn(Optional.of(Team.builder().id("by-name").title("ByName").build()));

        Optional<Team> team = resolver.resolveOddsApi(100, "Other Name");

        assertTrue(team.isPresent());
        assertEquals("by-id", team.get().getId());
    }

    @Test
    @DisplayName("resolveOddsApi falls back to name when id is missing")
    void resolveOddsApi_fallsBackToName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasId("odds-api.io", 999))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Brighton & Hove Albion"))
                .thenReturn(Optional.of(Team.builder().id("bha1").title("Brighton").build()));

        Optional<Team> team = resolver.resolveOddsApi(999, "Brighton & Hove Albion");

        assertTrue(team.isPresent());
        assertEquals("bha1", team.get().getId());
    }

    @Test
    @DisplayName("resolveTwentyFourScoreByName matches by saved external alias name")
    void resolveTwentyFourScoreByName_matchesByAliasName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("24score.pro", "Турция"))
                .thenReturn(Optional.of(Team.builder().id("tur1").title("Turkey").build()));

        Optional<Team> team = resolver.resolveTwentyFourScoreByName("Турция");

        assertTrue(team.isPresent());
        assertEquals("tur1", team.get().getId());
    }

    @Test
    @DisplayName("resolveFourScoreByName matches by saved external alias name")
    void resolveFourScoreByName_matchesByAliasName() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("4score.ru", "Англия"))
                .thenReturn(Optional.of(Team.builder().id("eng1").title("England").build()));

        Optional<Team> team = resolver.resolveFourScoreByName("Англия");

        assertTrue(team.isPresent());
        assertEquals("eng1", team.get().getId());
    }

    @Test
    @DisplayName("teamMatchesScoreProviderSide matches only same-provider alias")
    void teamMatchesScoreProviderSide_matchesByAliasName() {
        resolver = new TeamAliasResolver(teamsRepository);
        Team england = Team.builder()
                .id("eng1")
                .title("England")
                .externalAliases(List.of(
                        net.friendly_bets.models.TeamExternalAlias.builder()
                                .provider(MatchDataProviders.FOURSCORE)
                                .externalName("Англия")
                                .build()
                ))
                .build();

        assertTrue(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.FOURSCORE, "Англия"));
        assertFalse(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.FOURSCORE, "Франция"));
        assertFalse(resolver.teamMatchesScoreProviderSide(england, MatchDataProviders.TWENTYFOUR_SCORE, "Англия"));
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
    @DisplayName("resolveWc26Code tries catalog alternates for odds-api name")
    void resolveWc26Code_triesOddsApiAlternates() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Czechia"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Czech Republic"))
                .thenReturn(Optional.of(Team.builder().id("cze1").title("CzechRepublic").build()));

        Optional<Team> team = resolver.resolveWc26Code("CZE");

        assertTrue(team.isPresent());
        assertEquals("cze1", team.get().getId());
    }

    @Test
    @DisplayName("resolveWc26Code returns empty when odds-api alias is missing")
    void resolveWc26Code_emptyWhenNoAliasMatches() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Korea Republic"))
                .thenReturn(Optional.empty());
        when(teamsRepository.findByExternalAliasName("odds-api.io", "South Korea"))
                .thenReturn(Optional.empty());

        assertTrue(resolver.resolveWc26Code("KOR").isEmpty());
    }

    @Test
    @DisplayName("oddsApiAliasesMapped requires both id and name aliases when present")
    void oddsApiAliasesMapped_requiresBothWhenPresent() {
        resolver = new TeamAliasResolver(teamsRepository);
        when(teamsRepository.findByExternalAliasId("odds-api.io", 42))
                .thenReturn(Optional.of(Team.builder().id("t1").title("T1").build()));
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Team A"))
                .thenReturn(Optional.of(Team.builder().id("t1").title("T1").build()));
        when(teamsRepository.findByExternalAliasName("odds-api.io", "Team B"))
                .thenReturn(Optional.empty());

        assertTrue(resolver.oddsApiAliasesMapped(42, "Team A"));
        assertFalse(resolver.oddsApiAliasesMapped(42, "Team B"));
    }
}
