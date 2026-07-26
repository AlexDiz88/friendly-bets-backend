package net.friendly_bets.marathonbet;

import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.TeamAliasResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarathonbetEventMatcherTest {

    @Mock
    TeamAliasResolver teamAliasResolver;
    @Mock
    ErrorLogService errorLogService;
    @Mock
    OddsRepository oddsRepository;
    @Mock
    MarathonbetProperties properties;

    MarathonbetEventMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new MarathonbetEventMatcher(teamAliasResolver, errorLogService, oddsRepository, properties);
        when(properties.getEventWindowHours()).thenReturn(6);
        when(oddsRepository.findByMatchScheduleId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void resolve_whenNoEventsInKickoffWindow_returnsNoBookieEventWithoutErrorLog() {
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-md2")
                .homeTeamId("home")
                .awayTeamId("away")
                .utcKickoff(Instant.ofEpochMilli(1_800_000_000_000L))
                .build();
        MarathonbetPrematchEvent md1 = MarathonbetPrematchEvent.builder()
                .treeId(1L)
                .homeTeam("Арсенал")
                .awayTeam("Ковентри Сити")
                .displayTimeMillis(1_787_338_800_000L)
                .build();

        MarathonbetEventResolveResult result = matcher.resolveAndRecordMappingIssue(
                match, List.of(md1), "EPL", "2026", 2);

        assertFalse(result.isMatched());
        assertEquals(MarathonbetEventResolveResult.MissKind.NO_BOOKIE_EVENT, result.getMissKind());
        verify(errorLogService, never()).recordEventMappingMissing(any(), any(), any(), any(), any(Integer.class), any());
    }

    @Test
    void resolve_whenCandidatesButNoAlias_returnsMappingFailure() {
        Instant kickoff = Instant.ofEpochMilli(1_787_338_800_000L);
        MatchSchedule match = MatchSchedule.builder()
                .id("ms-1")
                .homeTeamId("home-id")
                .awayTeamId("away-id")
                .utcKickoff(kickoff)
                .build();
        MarathonbetPrematchEvent event = MarathonbetPrematchEvent.builder()
                .treeId(1L)
                .homeTeam("Арсенал")
                .awayTeam("Ковентри Сити")
                .displayTimeMillis(kickoff.toEpochMilli())
                .build();
        when(teamAliasResolver.resolveMarathonbetByName(anyString())).thenReturn(Optional.empty());

        MarathonbetEventResolveResult result = matcher.resolveAndRecordMappingIssue(
                match, List.of(event), "EPL", "2026", 1);

        assertFalse(result.isMatched());
        assertNull(result.getEvent());
        assertEquals(MarathonbetEventResolveResult.MissKind.MAPPING_FAILURE, result.getMissKind());
        verify(errorLogService).recordEventMappingMissing(
                match, "marathonbet", "EPL", "2026", 1, null);
    }
}
