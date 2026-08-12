package net.friendly_bets.providers;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ExternalApiHttpException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataProvider;
import net.friendly_bets.scrape.ExternalApiHttpFailures;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayerProviderRouterTest {

  @Mock
  ExternalDataLayerConfigService configService;
  @Mock
  LayerProviderRegistry registry;
  @Mock
  ErrorLogService errorLogService;

  LayerProviderRouter router;

  @BeforeEach
  void setUp() {
    router = new LayerProviderRouter(configService, registry, errorLogService);
  }

  @Test
  void schedule_failoverOnlyOnHttpTransportFailure() {
    StubScheduleProvider primary = new StubScheduleProvider("primary");
    StubScheduleProvider secondary = new StubScheduleProvider("secondary");
    primary.failWith = new BadRequestException("currentMatchdayUnresolved");
    assign("primary", "secondary");
    when(registry.findAs("primary", ScheduleProvider.class)).thenReturn(java.util.Optional.of(primary));

    assertThrows(BadRequestException.class, () ->
        router.execute(ExternalDataLayer.SCHEDULE, ScheduleProvider.class, p -> p.syncByLeagueCode("EPL", null)));
    verify(registry, never()).findAs("secondary", ScheduleProvider.class);
  }

  @Test
  void schedule_failoverWhenPrimaryHttpFails() {
    StubScheduleProvider primary = new StubScheduleProvider("primary");
    StubScheduleProvider secondary = new StubScheduleProvider("secondary");
    primary.failWith = ExternalApiHttpFailures.fetchFailed("soccer365FetchFailed");
    assign("primary", "secondary");
    when(registry.findAs("primary", ScheduleProvider.class)).thenReturn(java.util.Optional.of(primary));
    when(registry.findAs("secondary", ScheduleProvider.class)).thenReturn(java.util.Optional.of(secondary));

    net.friendly_bets.dto.ScheduleSyncResultDto result = router.execute(
        ExternalDataLayer.SCHEDULE,
        ScheduleProvider.class,
        p -> p.syncByLeagueCode("EPL", null));
    assertEquals("secondary", result.getLeagueCode());
  }

  @Test
  void live_failoverOnlyOnHttpTransportFailure() {
    StubLiveProvider primary = new StubLiveProvider("primary");
    StubLiveProvider secondary = new StubLiveProvider("secondary");
    primary.failWith = new BadRequestException("mappingFailed");
    assignLive("primary", "secondary");
    when(registry.findAs("primary", LiveMatchProvider.class)).thenReturn(java.util.Optional.of(primary));

    assertThrows(BadRequestException.class, () ->
        router.execute(ExternalDataLayer.LIVE, LiveMatchProvider.class, p -> p.syncLive(null)));
    verify(registry, never()).findAs("secondary", LiveMatchProvider.class);
  }

  @Test
  void live_failoverWhenPrimaryHttpFails() {
    StubLiveProvider primary = new StubLiveProvider("primary");
    StubLiveProvider secondary = new StubLiveProvider("secondary");
    primary.failWith = ExternalApiHttpFailures.fetchFailed("twentyFourScoreFetchFailed");
    assignLive("primary", "secondary");
    when(registry.findAs("primary", LiveMatchProvider.class)).thenReturn(java.util.Optional.of(primary));
    when(registry.findAs("secondary", LiveMatchProvider.class)).thenReturn(java.util.Optional.of(secondary));

    LiveMatchProvider.LiveSyncResult result = router.execute(
        ExternalDataLayer.LIVE,
        LiveMatchProvider.class,
        p -> p.syncLive(null));
    assertSame(secondary.result, result);
  }

  private void assign(String primaryId, String secondaryId) {
    when(configService.assignment(ExternalDataLayer.SCHEDULE)).thenReturn(
        AppSettings.LayerAssignment.builder()
            .primaryProvider(primaryId)
            .secondaryProvider(secondaryId)
            .build());
  }

  private void assignLive(String primaryId, String secondaryId) {
    when(configService.assignment(ExternalDataLayer.LIVE)).thenReturn(
        AppSettings.LayerAssignment.builder()
            .primaryProvider(primaryId)
            .secondaryProvider(secondaryId)
            .build());
  }

  static final class StubScheduleProvider implements ScheduleProvider {
    final String id;
    RuntimeException failWith;

    StubScheduleProvider(String id) {
      this.id = id;
    }

    @Override
    public String providerId() {
      return id;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
      return ExternalDataProvider.of(ExternalDataLayer.SCHEDULE);
    }

    @Override
    public net.friendly_bets.dto.ScheduleSyncResultDto syncLeague(
        net.friendly_bets.models.Season season,
        net.friendly_bets.models.League league,
        boolean respectFarKickoffSkip
    ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public net.friendly_bets.dto.ScheduleSyncResultDto syncByLeagueCode(String leagueCode, Integer matchday) {
      if (failWith != null) {
        throw failWith;
      }
      return net.friendly_bets.dto.ScheduleSyncResultDto.builder().leagueCode(id).build();
    }
  }

  static final class StubLiveProvider implements LiveMatchProvider {
    final String id;
    RuntimeException failWith;
    final LiveSyncResult result = new LiveSyncResult(0, 0, 0, 0, null, java.util.List.of(), java.util.List.of());

    StubLiveProvider(String id) {
      this.id = id;
    }

    @Override
    public String providerId() {
      return id;
    }

    @Override
    public Set<ExternalDataLayer> capabilities() {
      return ExternalDataProvider.of(ExternalDataLayer.LIVE);
    }

    @Override
    public LiveSyncResult syncLive(net.friendly_bets.models.Season season) {
      if (failWith != null) {
        throw failWith;
      }
      return result;
    }
  }
}
