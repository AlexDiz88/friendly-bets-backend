package net.friendly_bets.oddsapi;

import net.friendly_bets.marathonbet.MarathonbetSyncResult;
import net.friendly_bets.marathonbet.MarathonbetSyncService;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsSyncCoordinatorTest {

    @Mock
    MarathonbetProperties marathonbetProperties;
    @Mock
    MarathonbetSyncService marathonbetSyncService;
    @Mock
    OddsApiSyncService oddsApiSyncService;

    @InjectMocks
    OddsSyncCoordinator coordinator;

    @Test
    void marathonTournamentFailed_runsFullOddsApiTick() {
        when(marathonbetProperties.isSyncEnabled()).thenReturn(true);
        when(marathonbetProperties.isFallbackToOddsApi()).thenReturn(true);
        when(marathonbetSyncService.runTick()).thenReturn(
                MarathonbetSyncResult.builder().tournamentFetched(false).build()
        );

        coordinator.runScheduledSync();

        verify(oddsApiSyncService).runTick();
        verify(oddsApiSyncService, never()).runTickExcludingLeagues(List.of("WC"));
    }

    @Test
    void marathonOk_excludesWcFromOddsApiTick() {
        when(marathonbetProperties.isSyncEnabled()).thenReturn(true);
        when(marathonbetProperties.isFallbackToOddsApi()).thenReturn(true);
        when(marathonbetSyncService.runTick()).thenReturn(
                MarathonbetSyncResult.builder()
                        .tournamentFetched(true)
                        .matchesEligible(2)
                        .matchesMatched(2)
                        .mergedSaved(2)
                        .build()
        );

        coordinator.runScheduledSync();

        verify(oddsApiSyncService).runTickExcludingLeagues(List.of("WC"));
        verify(oddsApiSyncService, never()).runTick();
    }
}
