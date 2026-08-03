package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/**
 * Persists {@code enabled=false} for a layer without depending on {@link LayerProviderRegistry}.
 * Used by the scrape circuit breaker to avoid a Spring bean cycle:
 * ConfigService → Registry → Providers → HttpClients → CircuitBreaker → ConfigService.
 */
@Service
@RequiredArgsConstructor
public class ExternalDataLayerAutoDisableService {

    private final AppSettingsService appSettingsService;

    /**
     * Persist {@code enabled=false} for a layer (circuit breaker). Returns true when the flag changed.
     */
    @Transactional
    public boolean disableLayer(ExternalDataLayer layer, String reason) {
        if (layer == null) {
            return false;
        }
        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ExternalDataLayersBlock block = settings.getExternalDataLayers();
        if (block == null || block.getLayers() == null) {
            block = appSettingsService.defaultExternalDataLayers();
            settings.setExternalDataLayers(block);
        }
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        if (block.getLayers() != null) {
            layers.putAll(block.getLayers());
        }
        for (ExternalDataLayer l : ExternalDataLayer.values()) {
            layers.putIfAbsent(l, appSettingsService.defaultLayerAssignment(l));
        }
        AppSettings.LayerAssignment current = layers.get(layer);
        if (current != null && Boolean.FALSE.equals(current.getEnabled())) {
            return false;
        }
        AppSettings.LayerAssignment next = AppSettings.LayerAssignment.builder()
                .enabled(false)
                .primaryProvider(current != null ? current.getPrimaryProvider() : null)
                .secondaryProvider(current != null ? current.getSecondaryProvider() : null)
                .build();
        layers.put(layer, next);
        settings.setExternalDataLayers(AppSettings.ExternalDataLayersBlock.builder()
                .layers(layers)
                .oddsRefreshWithinHours(block.getOddsRefreshWithinHours())
                .build());
        appSettingsService.save(settings);
        return true;
    }
}
