package net.friendly_bets.footballdata;

import net.friendly_bets.dto.UnmappedExternalTeamNameDto;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.ApiSyncIssue;
import net.friendly_bets.repositories.ApiSyncIssueRepository;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiSyncIssueServiceTest {

    @Mock
    ApiSyncIssueRepository apiSyncIssueRepository;

    @Mock
    TeamAliasResolver teamAliasResolver;

    @InjectMocks
    ApiSyncIssueService apiSyncIssueService;

    @Test
    @DisplayName("getUnmappedTeamNameHints keeps separate chips per provider when external name matches")
    void getUnmappedTeamNameHints_sameNameDifferentProviders() {
        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(
                ApiSyncIssue.builder()
                        .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                        .provider(MatchDataProviders.FOOTBALL_DATA)
                        .homeTeamName("United States")
                        .homeTeamExternalId("123")
                        .build(),
                ApiSyncIssue.builder()
                        .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                        .provider(MatchDataProviders.ODDS_API)
                        .homeTeamName("United States")
                        .homeTeamExternalId("456")
                        .build()
        ));
        when(teamAliasResolver.resolveFootballData(123, "United States")).thenReturn(Optional.empty());
        when(teamAliasResolver.oddsApiAliasesMapped(456, "United States")).thenReturn(false);

        List<UnmappedExternalTeamNameDto> hints = apiSyncIssueService.getUnmappedTeamNameHints();

        assertEquals(2, hints.size());
        assertTrue(hints.stream().anyMatch(h ->
                MatchDataProviders.FOOTBALL_DATA.equals(h.getProvider())
                        && "United States".equals(h.getExternalName())
                        && Integer.valueOf(123).equals(h.getExternalId())));
        assertTrue(hints.stream().anyMatch(h ->
                MatchDataProviders.ODDS_API.equals(h.getProvider())
                        && "United States".equals(h.getExternalName())
                        && Integer.valueOf(456).equals(h.getExternalId())));
    }

    @Test
    @DisplayName("purgeTeamMappingIssuesForExternalTeam removes only matching resolved issues")
    void purgeTeamMappingIssuesForExternalTeam_removesMatchingOnly() {
        ApiSyncIssue mexicoIssue = ApiSyncIssue.builder()
                .id("mexico")
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .provider(MatchDataProviders.ODDS_API)
                .homeTeamName("Mexico")
                .homeTeamExternalId("4781")
                .build();
        ApiSyncIssue paraguayIssue = ApiSyncIssue.builder()
                .id("paraguay")
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .provider(MatchDataProviders.ODDS_API)
                .awayTeamName("Paraguay")
                .awayTeamExternalId("4789")
                .build();
        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc())
                .thenReturn(List.of(mexicoIssue, paraguayIssue));
        when(teamAliasResolver.oddsApiAliasesMapped(4781, "Mexico")).thenReturn(true);

        int removed = apiSyncIssueService.purgeTeamMappingIssuesForExternalTeam(
                MatchDataProviders.ODDS_API,
                "Mexico",
                4781
        );

        assertEquals(1, removed);
        verify(apiSyncIssueRepository).deleteAllById(List.of("mexico"));
    }

    @Test
    @DisplayName("getLatest purges resolved team-mapping issues before returning")
    void getLatest_purgesResolvedTeamMappingIssues() {
        ApiSyncIssue resolved = ApiSyncIssue.builder()
                .id("resolved")
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .provider(MatchDataProviders.ODDS_API)
                .homeTeamName("Mexico")
                .homeTeamExternalId("4781")
                .build();
        ApiSyncIssue pending = ApiSyncIssue.builder()
                .id("pending")
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .provider(MatchDataProviders.ODDS_API)
                .awayTeamName("Paraguay")
                .awayTeamExternalId("4789")
                .build();
        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc())
                .thenReturn(List.of(resolved, pending))
                .thenReturn(List.of(pending));
        when(teamAliasResolver.oddsApiAliasesMapped(4781, "Mexico")).thenReturn(true);
        when(teamAliasResolver.oddsApiAliasesMapped(4789, "Paraguay")).thenReturn(false);

        List<ApiSyncIssue> latest = apiSyncIssueService.getLatest();

        verify(apiSyncIssueRepository).deleteAllById(List.of("resolved"));
        assertEquals(1, latest.size());
        assertEquals("pending", latest.get(0).getId());
    }

    @Test
    @DisplayName("deleteById throws when issue missing")
    void deleteById_notFound() {
        when(apiSyncIssueRepository.existsById("missing")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> apiSyncIssueService.deleteById("missing"));
    }

    @Test
    @DisplayName("recordMissingTeamMapping creates separate issues for each unmapped team")
    void recordMissingTeamMapping_createsPerTeamIssues() {
        FootballDataMatchDto matchDto = new FootballDataMatchDto();
        matchDto.setId(42L);
        FootballDataMatchDto.Team home = new FootballDataMatchDto.Team();
        home.setId(1);
        home.setName("Home FC");
        FootballDataMatchDto.Team away = new FootballDataMatchDto.Team();
        away.setId(2);
        away.setName("Away FC");
        matchDto.setHomeTeam(home);
        matchDto.setAwayTeam(away);

        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(teamAliasResolver.resolveFootballData(1, "Home FC")).thenReturn(Optional.empty());
        when(teamAliasResolver.resolveFootballData(2, "Away FC")).thenReturn(Optional.empty());

        apiSyncIssueService.recordMissingTeamMapping("PL", "2025", 3, matchDto);

        ArgumentCaptor<ApiSyncIssue> captor = ArgumentCaptor.forClass(ApiSyncIssue.class);
        verify(apiSyncIssueRepository, times(2)).save(captor.capture());
        List<ApiSyncIssue> saved = captor.getAllValues();
        assertEquals("Home FC", saved.get(0).getHomeTeamName());
        assertNull(saved.get(0).getAwayTeamName());
        assertEquals("Away FC", saved.get(1).getAwayTeamName());
        assertNull(saved.get(1).getHomeTeamName());
    }

    @Test
    @DisplayName("recordMissingTeamMapping skips mapped team and records only missing side")
    void recordMissingTeamMapping_skipsMappedTeam() {
        FootballDataMatchDto matchDto = new FootballDataMatchDto();
        matchDto.setId(42L);
        FootballDataMatchDto.Team home = new FootballDataMatchDto.Team();
        home.setId(1);
        home.setName("Home FC");
        FootballDataMatchDto.Team away = new FootballDataMatchDto.Team();
        away.setId(2);
        away.setName("Away FC");
        matchDto.setHomeTeam(home);
        matchDto.setAwayTeam(away);

        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(teamAliasResolver.resolveFootballData(1, "Home FC")).thenReturn(Optional.of(new Team()));
        when(teamAliasResolver.resolveFootballData(2, "Away FC")).thenReturn(Optional.empty());

        apiSyncIssueService.recordMissingTeamMapping("PL", "2025", 3, matchDto);

        ArgumentCaptor<ApiSyncIssue> captor = ArgumentCaptor.forClass(ApiSyncIssue.class);
        verify(apiSyncIssueRepository, times(1)).save(captor.capture());
        assertEquals("Away FC", captor.getValue().getAwayTeamName());
        assertNull(captor.getValue().getHomeTeamName());
    }

    @Test
    @DisplayName("getUnmappedTeamNameHints keeps away chip when legacy issue has mapped home side")
    void getUnmappedTeamNameHints_legacyIssueKeepsUnmappedAway() {
        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(
                ApiSyncIssue.builder()
                        .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                        .provider(MatchDataProviders.FOOTBALL_DATA)
                        .homeTeamName("Home FC")
                        .homeTeamExternalId("1")
                        .awayTeamName("Away FC")
                        .awayTeamExternalId("2")
                        .build()
        ));
        when(teamAliasResolver.resolveFootballData(1, "Home FC")).thenReturn(Optional.of(new Team()));
        when(teamAliasResolver.resolveFootballData(2, "Away FC")).thenReturn(Optional.empty());

        List<UnmappedExternalTeamNameDto> hints = apiSyncIssueService.getUnmappedTeamNameHints();

        assertEquals(1, hints.size());
        assertEquals("Away FC", hints.get(0).getExternalName());
        assertEquals(Integer.valueOf(2), hints.get(0).getExternalId());
        verify(apiSyncIssueRepository, never()).deleteAllById(any());
    }

    @Test
    @DisplayName("purgeTeamMappingIssuesForExternalTeam trims legacy issue instead of deleting both sides")
    void purgeTeamMappingIssuesForExternalTeam_trimsLegacyCombinedIssue() {
        ApiSyncIssue legacyIssue = ApiSyncIssue.builder()
                .id("legacy")
                .issueType(ApiSyncIssue.IssueType.TEAM_MAPPING_MISSING.name())
                .provider(MatchDataProviders.FOOTBALL_DATA)
                .homeTeamName("Home FC")
                .homeTeamExternalId("1")
                .awayTeamName("Away FC")
                .awayTeamExternalId("2")
                .build();
        when(apiSyncIssueRepository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of(legacyIssue));
        when(teamAliasResolver.resolveFootballData(1, "Home FC")).thenReturn(Optional.of(new Team()));

        int affected = apiSyncIssueService.purgeTeamMappingIssuesForExternalTeam(
                MatchDataProviders.FOOTBALL_DATA,
                "Home FC",
                1
        );

        assertEquals(1, affected);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ApiSyncIssue>> captor = ArgumentCaptor.forClass(List.class);
        verify(apiSyncIssueRepository).saveAll(captor.capture());
        ApiSyncIssue saved = captor.getValue().get(0);
        assertNull(saved.getHomeTeamName());
        assertNull(saved.getHomeTeamExternalId());
        assertEquals("Away FC", saved.getAwayTeamName());
        verify(apiSyncIssueRepository, never()).deleteAllById(any());
    }
}
