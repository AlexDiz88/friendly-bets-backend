package net.friendly_bets.services;

import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TeamDisplayNames;
import net.friendly_bets.models.TeamExternalAlias;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.utils.TeamI18nCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalTeamAliasAutoBindServiceTest {

    @Mock
    RunningSeasonLookup runningSeasonLookup;
    @Mock
    LeaguesRepository leaguesRepository;
    @Mock
    TeamsRepository teamsRepository;
    @Mock
    TeamI18nCatalog teamI18nCatalog;
    @Mock
    ErrorLogService errorLogService;

    @InjectMocks
    ExternalTeamAliasAutoBindService service;

    private Team arsenal;

    @BeforeEach
    void setUp() {
        arsenal = Team.builder()
                .id("ars1")
                .title("Arsenal")
                .displayNames(TeamDisplayNames.builder().en("Arsenal").ru("Арсенал").build())
                .externalAliases(new ArrayList<>(List.of(
                        TeamExternalAlias.builder()
                                .provider(ExternalProviderIds.FLASHSCORE)
                                .externalName("Арсенал")
                                .build()
                )))
                .build();

        League league = League.builder()
                .leagueCode(League.LeagueCode.EPL)
                .teams(List.of(arsenal))
                .build();
        Season season = Season.builder()
                .id("s1")
                .leagues(List.of(league))
                .build();

        when(runningSeasonLookup.findRunningSeasonOrThrow(any())).thenReturn(season);
        when(teamsRepository.findById("ars1")).thenReturn(Optional.of(arsenal));
        when(teamI18nCatalog.resolveByTitle(any())).thenReturn(null);
    }

    @Test
    void bindAndCollectUnmapped_countsMismatchAndLogsSummaryWithoutOverwrite() {
        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.FLASHSCORE,
                "EPL",
                List.of("Arsenal"),
                false
        );

        assertEquals(1, result.getMismatchCount());
        assertEquals(0, result.getOverwrittenCount());
        assertEquals(1, result.getAlreadyMappedCount());
        assertEquals("Арсенал", findFlashscoreAlias(arsenal).getExternalName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErrorLogService.TeamAliasMismatchDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(errorLogService).recordTeamAliasMismatchSummary(
                eq(ExternalProviderIds.FLASHSCORE),
                eq("EPL"),
                captor.capture(),
                eq(false)
        );
        assertEquals(1, captor.getValue().size());
        assertEquals("Арсенал", captor.getValue().get(0).getCurrentAlias());
        assertEquals("Arsenal", captor.getValue().get(0).getIncomingAlias());
        verify(teamsRepository, never()).save(any());
    }

    @Test
    void bindAndCollectUnmapped_forceOverwriteReplacesAliasAndCountsOverwritten() {
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.FLASHSCORE,
                "EPL",
                List.of("Arsenal"),
                true
        );

        assertEquals(1, result.getMismatchCount());
        assertEquals(1, result.getOverwrittenCount());
        assertEquals(0, result.getAlreadyMappedCount());
        assertEquals("Arsenal", findFlashscoreAlias(arsenal).getExternalName());
        verify(teamsRepository).save(arsenal);
        verify(errorLogService).recordTeamAliasMismatchSummary(
                eq(ExternalProviderIds.FLASHSCORE),
                eq("EPL"),
                any(),
                eq(true)
        );
    }

    @Test
    void bindAndCollectUnmapped_newAliasAutoBindsWithoutMismatchLog() {
        arsenal.setExternalAliases(new ArrayList<>());
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.FLASHSCORE,
                "EPL",
                List.of("Arsenal"),
                false
        );

        assertEquals(1, result.getAutoBoundCount());
        assertEquals(0, result.getMismatchCount());
        verify(errorLogService, never()).recordTeamAliasMismatchSummary(any(), any(), any(), any(Boolean.class));
    }

    @Test
    void bindAndCollectUnmapped_matchesDisplayNameInSameLeague() {
        arsenal.setExternalAliases(new ArrayList<>());
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        League clLeague = League.builder()
                .leagueCode(League.LeagueCode.CL)
                .teams(List.of(arsenal))
                .build();
        Season season = Season.builder()
                .id("s1")
                .leagues(List.of(clLeague))
                .build();
        when(runningSeasonLookup.findRunningSeasonOrThrow(any())).thenReturn(season);
        when(teamsRepository.findById("ars1")).thenReturn(Optional.of(arsenal));

        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.MARATHONBET,
                "CL",
                List.of("Арсенал"),
                false
        );

        assertEquals(1, result.getAutoBoundCount());
        assertEquals(0, result.getUnmapped().size());
        assertTrue(arsenal.getExternalAliases().stream()
                .anyMatch(a -> ExternalProviderIds.MARATHONBET.equals(a.getProvider())
                        && "Арсенал".equals(a.getExternalName())));
    }

    @Test
    void bindAndCollectUnmapped_matchesTeamTitleWhenDisplayNamesEmpty() {
        Team bayern = Team.builder()
                .id("bay1")
                .title("BayernMunich")
                .displayNames(null)
                .externalAliases(new ArrayList<>())
                .build();
        League league = League.builder()
                .leagueCode(League.LeagueCode.CL)
                .teams(List.of(bayern))
                .build();
        Season season = Season.builder()
                .id("s1")
                .leagues(List.of(league))
                .build();
        when(runningSeasonLookup.findRunningSeasonOrThrow(any())).thenReturn(season);
        when(teamsRepository.findById("bay1")).thenReturn(Optional.of(bayern));
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.MARATHONBET,
                "CL",
                List.of("BayernMunich"),
                false
        );

        assertEquals(1, result.getAutoBoundCount());
    }

    @Test
    void bindAndCollectUnmapped_matchesBundledI18nRuWhenDisplayNamesEmpty() {
        Team roma = Team.builder()
                .id("roma1")
                .title("Roma")
                .displayNames(null)
                .externalAliases(new ArrayList<>())
                .build();
        League clLeague = League.builder()
                .leagueCode(League.LeagueCode.CL)
                .teams(List.of(roma))
                .build();
        Season season = Season.builder()
                .id("s1")
                .leagues(List.of(clLeague))
                .build();
        when(runningSeasonLookup.findRunningSeasonOrThrow(any())).thenReturn(season);
        when(teamsRepository.findById("roma1")).thenReturn(Optional.of(roma));
        when(teamsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamI18nCatalog.resolveByTitle("Roma")).thenReturn(
                TeamDisplayNames.builder().en("Roma").ru("Рома").de("AS Rom").build()
        );

        var result = service.bindAndCollectUnmapped(
                ExternalProviderIds.SPORTS_RU,
                "CL",
                List.of("Рома"),
                false
        );

        assertEquals(1, result.getAutoBoundCount());
        assertEquals(0, result.getUnmapped().size());
        assertTrue(roma.getExternalAliases().stream()
                .anyMatch(a -> ExternalProviderIds.SPORTS_RU.equals(a.getProvider())
                        && "Рома".equals(a.getExternalName())));
    }

    private static TeamExternalAlias findFlashscoreAlias(Team team) {
        return team.getExternalAliases().stream()
                .filter(a -> ExternalProviderIds.FLASHSCORE.equals(a.getProvider()))
                .findFirst()
                .orElseThrow();
    }
}
