package net.friendly_bets.services;

import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.GameScore;
import net.friendly_bets.models.User;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.UsersRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchFinalizeOrchestratorOrphanSettleTest {

    @Mock LayerProviderRouter router;
    @Mock MatchScheduleRepository matchScheduleRepository;
    @Mock BetsRepository betsRepository;
    @Mock BetsService betsService;
    @Mock StatsService statsService;
    @Mock UsersRepository usersRepository;
    @Mock ExternalDataLayerConfigService layerConfigService;
    @Mock ExternalApiMonitoringService monitoringService;
    @Mock ErrorLogService errorLogService;
    @Mock StandingsSyncOrchestrator standingsSyncOrchestrator;

    MatchResultSyncProperties properties;
    MatchFinalizeOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new MatchResultSyncProperties();
        properties.setAutoSettleEnabled(true);
        orchestrator = new MatchFinalizeOrchestrator(
                router,
                matchScheduleRepository,
                betsRepository,
                betsService,
                statsService,
                properties,
                usersRepository,
                layerConfigService,
                monitoringService,
                errorLogService,
                standingsSyncOrchestrator
        );
        orchestrator.setSelf(orchestrator);
    }

    @Test
    void isFinalizedForSettle_requiresFirstTime() {
        MatchSchedule ftOnly = MatchSchedule.builder()
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("3:1").build())
                .build();
        assertFalse(MatchScheduleSettleService.isFinalizedForSettle(ftOnly));

        MatchSchedule complete = MatchSchedule.builder()
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("3:1").firstTime("2:0").build())
                .build();
        assertTrue(MatchScheduleSettleService.isFinalizedForSettle(complete));
    }

    @Test
    void settleOrphanFinalizedMatches_settlesWhenOpenedBetsExist() {
        MatchSchedule orphan = MatchSchedule.builder()
                .id("ms-orphan")
                .seasonId("season-1")
                .leagueId("league-1")
                .homeTeamId("home")
                .awayTeamId("away")
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("3:1").firstTime("2:0").build())
                .build();
        when(matchScheduleRepository.findBySeasonId("season-1")).thenReturn(List.of(orphan));
        when(matchScheduleRepository.findById("ms-orphan")).thenReturn(Optional.of(orphan));
        when(betsRepository.findAllByMatchScheduleIdAndBetStatusIn("ms-orphan", List.of(Bet.BetStatus.OPENED)))
                .thenReturn(List.of(Bet.builder().id("bet-1").betStatus(Bet.BetStatus.OPENED).build()));
        when(usersRepository.findFirstByRoleOrderByCreatedAtAsc(User.Role.ADMIN))
                .thenReturn(Optional.of(User.builder().id("admin-1").build()));
        when(betsService.setBetResults(eq("admin-1"), eq("season-1"), anyList(), eq(false)))
                .thenReturn(BetsPage.builder().bets(List.of()).build());

        int count = orchestrator.settleOrphanFinalizedMatches("season-1");

        assertEquals(1, count);
        verify(betsService).setBetResults(eq("admin-1"), eq("season-1"), anyList(), eq(false));
    }

    @Test
    void settleOrphanFinalizedMatches_skipsWhenNoOpenedBets() {
        MatchSchedule done = MatchSchedule.builder()
                .id("ms-done")
                .seasonId("season-1")
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("1:0").firstTime("0:0").build())
                .build();
        when(matchScheduleRepository.findBySeasonId("season-1")).thenReturn(List.of(done));
        when(betsRepository.findAllByMatchScheduleIdAndBetStatusIn("ms-done", List.of(Bet.BetStatus.OPENED)))
                .thenReturn(List.of());

        int count = orchestrator.settleOrphanFinalizedMatches("season-1");

        assertEquals(0, count);
        verify(betsService, never()).setBetResults(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void settleOrphanFinalizedMatches_skipsWhenBlocked() {
        MatchSchedule blocked = MatchSchedule.builder()
                .id("ms-blocked")
                .seasonId("season-1")
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("1:0").firstTime("0:0").build())
                .settleNextAttemptAt(Instant.now().plusSeconds(3600))
                .build();
        when(matchScheduleRepository.findBySeasonId("season-1")).thenReturn(List.of(blocked));

        int count = orchestrator.settleOrphanFinalizedMatches("season-1");

        assertEquals(0, count);
        verify(betsRepository, never()).findAllByMatchScheduleIdAndBetStatusIn(any(), anyList());
        verify(betsService, never()).setBetResults(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void settleOrphanFinalizedMatches_recordsFailureAndBlocksAfterMax() {
        properties.setSettleMaxFailures(3);
        properties.setSettleBlockAfterMaxMs(3_600_000L);
        MatchSchedule orphan = MatchSchedule.builder()
                .id("ms-fail")
                .seasonId("season-1")
                .leagueId("league-1")
                .homeTeamId("home")
                .awayTeamId("away")
                .finalizedAt(Instant.now())
                .gameScore(GameScore.builder().fullTime("3:1").firstTime("2:0").build())
                .settleFailCount(2)
                .build();
        when(matchScheduleRepository.findBySeasonId("season-1")).thenReturn(List.of(orphan));
        when(matchScheduleRepository.findById("ms-fail")).thenReturn(Optional.of(orphan));
        when(betsRepository.findAllByMatchScheduleIdAndBetStatusIn("ms-fail", List.of(Bet.BetStatus.OPENED)))
                .thenReturn(List.of(Bet.builder().id("bet-1").betStatus(Bet.BetStatus.OPENED).build()));
        when(usersRepository.findFirstByRoleOrderByCreatedAtAsc(User.Role.ADMIN))
                .thenReturn(Optional.of(User.builder().id("admin-1").build()));
        when(betsService.setBetResults(eq("admin-1"), eq("season-1"), anyList(), eq(false)))
                .thenThrow(new RuntimeException("boom"));

        int count = orchestrator.settleOrphanFinalizedMatches("season-1");

        assertEquals(0, count);
        verify(errorLogService).recordAutoSettleFailure(eq(orphan), eq("boom"));
        verify(matchScheduleRepository).save(orphan);
        assertEquals(0, orphan.getSettleFailCount());
        assertNotNull(orphan.getSettleNextAttemptAt());
    }
}
