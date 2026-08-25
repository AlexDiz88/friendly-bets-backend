package net.friendly_bets.services;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.config.MatchResultSyncProperties;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.FullMatchProvider;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.repositories.MatchScheduleRepository;
import net.friendly_bets.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchFinalizeOrchestratorFailureBackoffTest {

    @Mock
    LayerProviderRouter router;
    @Mock
    MatchScheduleRepository matchScheduleRepository;
    @Mock
    BetsService betsService;
    @Mock
    StatsService statsService;
    @Mock
    UsersRepository usersRepository;
    @Mock
    ExternalDataLayerConfigService layerConfigService;
    @Mock
    ExternalApiMonitoringService monitoringService;
    @Mock
    ErrorLogService errorLogService;
    @Mock
    StandingsSyncOrchestrator standingsSyncOrchestrator;

    MatchResultSyncProperties properties;
    MatchFinalizeOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        properties = new MatchResultSyncProperties();
        properties.setFullMatchInitialDelayMs(300_000L);
        properties.setFullMatchRetryDelayMs(300_000L);
        properties.setFullMatchHourlyDelayMs(3_600_000L);
        properties.setAutoSettleEnabled(false);
        orchestrator = new MatchFinalizeOrchestrator(
                router,
                matchScheduleRepository,
                betsService,
                statsService,
                properties,
                usersRepository,
                layerConfigService,
                monitoringService,
                errorLogService,
                standingsSyncOrchestrator
        );
    }

    @Test
    void notFound_defersFiveMinutes_doesNotRethrow() {
        Instant detected = Instant.parse("2026-08-24T20:57:22Z");
        MatchSchedule schedule = MatchSchedule.builder()
                .id("ms-1")
                .status("FINISHED")
                .liveFinishedDetectedAt(detected)
                .build();
        when(matchScheduleRepository.findById("ms-1")).thenReturn(Optional.of(schedule));
        when(layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH)).thenReturn(true);
        when(router.execute(eq(ExternalDataLayer.FULL_MATCH), eq(FullMatchProvider.class), any(Function.class)))
                .thenThrow(new BadRequestException("fullMatchNotFound"));
        when(matchScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        MatchSchedule result = orchestrator.finalizeFinishedMatch(schedule);
        Instant after = Instant.now();

        ArgumentCaptor<MatchSchedule> captor = ArgumentCaptor.forClass(MatchSchedule.class);
        verify(matchScheduleRepository).save(captor.capture());
        MatchSchedule saved = captor.getValue();
        assertEquals(1, saved.getFullMatchNotReadyCount());
        assertNotNull(saved.getFullMatchNextAttemptAt());
        Duration wait = Duration.between(before, saved.getFullMatchNextAttemptAt());
        assertTrue(wait.getSeconds() >= 299 && wait.getSeconds() <= 301 + Duration.between(before, after).getSeconds());
        assertEquals("ms-1", result.getId());
    }

    @Test
    void secondFailure_defersOneHour() {
        Instant detected = Instant.parse("2026-08-24T20:57:22Z");
        MatchSchedule schedule = MatchSchedule.builder()
                .id("ms-1")
                .status("FINISHED")
                .liveFinishedDetectedAt(detected)
                .fullMatchNotReadyCount(1)
                .build();
        when(matchScheduleRepository.findById("ms-1")).thenReturn(Optional.of(schedule));
        when(layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH)).thenReturn(true);
        when(router.execute(eq(ExternalDataLayer.FULL_MATCH), eq(FullMatchProvider.class), any(Function.class)))
                .thenThrow(new BadRequestException("fullMatchNotFound"));
        when(matchScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        orchestrator.finalizeFinishedMatch(schedule);

        ArgumentCaptor<MatchSchedule> captor = ArgumentCaptor.forClass(MatchSchedule.class);
        verify(matchScheduleRepository).save(captor.capture());
        Duration wait = Duration.between(before, captor.getValue().getFullMatchNextAttemptAt());
        assertTrue(wait.getSeconds() >= 3599);
        assertEquals(2, captor.getValue().getFullMatchNotReadyCount());
    }
}
