package net.friendly_bets.services;

import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataLayersUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDataLayerAutoDisableServiceTest {

    private final AtomicReference<AppSettings> stored = new AtomicReference<>();
    private final AtomicInteger events = new AtomicInteger();

    private ExternalDataLayerAutoDisableService service;

    @BeforeEach
    void setUp() {
        events.set(0);
        AppSettings.ExternalDataLayersBlock block = AppSettings.ExternalDataLayersBlock.builder()
                .layers(new EnumMap<>(ExternalDataLayer.class))
                .build();
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            block.getLayers().put(layer, AppSettings.LayerAssignment.builder()
                    .enabled(true)
                    .primaryProvider("x")
                    .build());
        }
        stored.set(AppSettings.builder().id(AppSettings.DEFAULT_ID).externalDataLayers(block).build());

        AppSettingsService appSettingsService = new AppSettingsService(null, null) {
            @Override
            public AppSettings getOrCreate() {
                return stored.get();
            }

            @Override
            public AppSettings save(AppSettings settings) {
                stored.set(settings);
                return settings;
            }

            @Override
            public AppSettings.ExternalDataLayersBlock defaultExternalDataLayers() {
                return stored.get().getExternalDataLayers();
            }

            @Override
            public AppSettings.LayerAssignment defaultLayerAssignment(ExternalDataLayer layer) {
                return AppSettings.LayerAssignment.builder().enabled(true).build();
            }
        };
        ApplicationEventPublisher publisher = event -> {
            if (event instanceof ExternalDataLayersUpdatedEvent) {
                events.incrementAndGet();
            }
        };
        service = new ExternalDataLayerAutoDisableService(appSettingsService, publisher);
    }

    @Test
    void disableLayer_setsStampAndDisables() {
        assertTrue(service.disableLayer(ExternalDataLayer.ODDS, "trip"));
        AppSettings.LayerAssignment a = stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS);
        assertFalse(a.getEnabled());
        assertNotNull(a.getCircuitDisabledAt());
        assertTrue(events.get() >= 1);
    }

    @Test
    void reenable_waitsForCooldownThenEnables() {
        service.disableLayer(ExternalDataLayer.ODDS, "trip");
        assertFalse(service.reenableIfCircuitCooldownElapsed(ExternalDataLayer.ODDS, Duration.ofHours(6)));
        assertTrue(service.isCircuitCooldownActive(ExternalDataLayer.ODDS, Duration.ofHours(6)));

        AppSettings.LayerAssignment a = stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS);
        a.setCircuitDisabledAt(Instant.now().minus(Duration.ofHours(7)));

        assertTrue(service.reenableIfCircuitCooldownElapsed(ExternalDataLayer.ODDS, Duration.ofHours(6)));
        AppSettings.LayerAssignment after = stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS);
        assertTrue(after.getEnabled());
        assertNull(after.getCircuitDisabledAt());
        assertFalse(service.isCircuitCooldownActive(ExternalDataLayer.ODDS, Duration.ofHours(6)));
    }

    @Test
    void adminStyleDisableWithoutStamp_isNotAutoReenabled() {
        AppSettings.LayerAssignment a = stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS);
        a.setEnabled(false);
        a.setCircuitDisabledAt(null);
        assertFalse(service.reenableIfCircuitCooldownElapsed(ExternalDataLayer.ODDS, Duration.ofHours(6)));
        assertFalse(stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS).getEnabled());
    }

    @Test
    void disableWhenAlreadyOff_stampsForCooldown() {
        AppSettings.LayerAssignment a = stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS);
        a.setEnabled(false);
        a.setCircuitDisabledAt(null);
        assertFalse(service.disableLayer(ExternalDataLayer.ODDS, "trip again"));
        assertNotNull(stored.get().getExternalDataLayers().getLayers().get(ExternalDataLayer.ODDS).getCircuitDisabledAt());
    }
}
