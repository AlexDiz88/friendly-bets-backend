package net.friendly_bets.oddsapi;

import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultSideSnapshot;
import net.friendly_bets.models.gameresults.GameResultSourceSnapshot;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsApiEventMatcherTest {

    @Mock
    private TeamAliasResolver teamAliasResolver;
    @Mock
    private GetEntityService getEntityService;
    @Mock
    private ApiSyncIssueService apiSyncIssueService;
    @Mock
    private GameResultRecordRepository gameResultRecordRepository;

    private OddsApiEventMatcher matcher;

    @BeforeEach
    void setUp() {
        OddsApiProperties properties = new OddsApiProperties();
        properties.setEventWindowHours(6);
        matcher = new OddsApiEventMatcher(
                teamAliasResolver,
                getEntityService,
                apiSyncIssueService,
                gameResultRecordRepository,
                properties
        );
    }

    @Test
    void resolve_usesCachedEventId() {
        GameResultRecord match = GameResultRecord.builder()
                .id("gr1")
                .oddsApiEventId(99L)
                .build();

        Optional<Long> result = matcher.resolveAndPersistEventId(match, List.of(), "EPL", "2025", 5);
        assertEquals(99L, result.orElseThrow());
    }

    @Test
    void resolve_matchesByNormalizedNamesAndPersists() {
        GameResultRecord match = buildMatch("gr2", "home-id", "away-id");
        OddsApiEventDto event = new OddsApiEventDto();
        event.setId(42L);
        event.setHome("Manchester United FC");
        event.setAway("Liverpool FC");
        event.setDate(match.getUtcDate().atZone(java.time.ZoneOffset.UTC).toString());

        when(teamAliasResolver.oddsApiAliasesMapped(any(), any())).thenReturn(true);
        when(teamAliasResolver.resolveOddsApiById(any())).thenReturn(Optional.empty());
        when(teamAliasResolver.resolveOddsApiByName(any())).thenReturn(Optional.empty());
        Team home = Team.builder().id("home-id").title("ManUnited").build();
        Team away = Team.builder().id("away-id").title("Liverpool").build();
        when(getEntityService.getTeamOrThrow("home-id")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("away-id")).thenReturn(away);

        Optional<Long> result = matcher.resolveAndPersistEventId(
                match,
                List.of(event),
                "EPL",
                "2025",
                5
        );

        assertEquals(42L, result.orElseThrow());
        assertEquals(42L, match.getOddsApiEventId());
        verify(gameResultRecordRepository).save(match);
    }

    @Test
    void resolve_countsTeamMappingIssuesWhenAliasMissing() {
        GameResultRecord match = buildMatch("gr4", "home-id", "away-id");
        OddsApiEventDto event = new OddsApiEventDto();
        event.setId(55L);
        event.setHome("Manchester United FC");
        event.setHomeId(9001);
        event.setAway("Liverpool FC");
        event.setAwayId(9002);
        event.setDate(match.getUtcDate().atZone(java.time.ZoneOffset.UTC).toString());

        when(teamAliasResolver.oddsApiAliasesMapped(any(), any())).thenReturn(false);
        when(teamAliasResolver.resolveOddsApiById(any())).thenReturn(Optional.empty());
        when(teamAliasResolver.resolveOddsApiByName(any())).thenReturn(Optional.empty());
        Team home = Team.builder().id("home-id").title("ManUnited").build();
        Team away = Team.builder().id("away-id").title("Liverpool").build();
        when(getEntityService.getTeamOrThrow("home-id")).thenReturn(home);
        when(getEntityService.getTeamOrThrow("away-id")).thenReturn(away);

        OddsTeamMappingCollector collector = new OddsTeamMappingCollector();
        Optional<Long> result = matcher.resolveAndPersistEventId(
                match,
                List.of(event),
                "EPL",
                "2025",
                5,
                collector
        );

        assertTrue(result.isPresent());
        assertEquals(2, collector.getIssueCount());
        verify(apiSyncIssueService).recordOddsTeamMappingMissing(
                eq(match), eq(true), eq("Manchester United FC"), eq(9001));
        verify(apiSyncIssueService).recordOddsTeamMappingMissing(
                eq(match), eq(false), eq("Liverpool FC"), eq(9002));
    }

    @Test
    void resolve_recordsIssueWhenNoEvent() {
        GameResultRecord match = buildMatch("gr3", "h", "a");
        when(teamAliasResolver.oddsApiAliasesMapped(any(), any())).thenReturn(true);
        when(teamAliasResolver.resolveOddsApiById(any())).thenReturn(Optional.empty());
        when(teamAliasResolver.resolveOddsApiByName(any())).thenReturn(Optional.empty());
        when(getEntityService.getTeamOrThrow(any())).thenReturn(Team.builder().id("x").title("X").build());

        Optional<Long> result = matcher.resolveAndPersistEventId(match, List.of(), "EPL", "2025", 1);
        assertTrue(result.isEmpty());
        verify(apiSyncIssueService).recordOddsEventMappingMissing(eq(match), eq("EPL"), eq("2025"), eq(1));
    }

    private static GameResultRecord buildMatch(String id, String homeTeamId, String awayTeamId) {
        LocalDateTime kickoff = LocalDateTime.now().plusDays(2);
        GameResultSourceSnapshot source = GameResultSourceSnapshot.builder()
                .home(GameResultSideSnapshot.builder().externalName("Manchester United").build())
                .away(GameResultSideSnapshot.builder().externalName("Liverpool").build())
                .build();
        return GameResultRecord.builder()
                .id(id)
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .utcDate(kickoff)
                .status("SCHEDULED")
                .sources(java.util.Map.of("football_data", source))
                .build();
    }
}
