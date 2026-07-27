package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.config.ExternalDataProperties;
import net.friendly_bets.repositories.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;
    private final ExternalDataProperties externalDataProperties;

    @Transactional
    public AppSettings getOrCreate() {
        return appSettingsRepository.findById(AppSettings.DEFAULT_ID)
                .orElseGet(this::createDefaults);
    }

    @Transactional
    public AppSettings save(AppSettings settings) {
        return appSettingsRepository.save(settings);
    }

    public AppSettings.LayerAssignment defaultLayerAssignment(ExternalDataLayer layer) {
        return switch (layer) {
            case SCHEDULE, FULL_MATCH -> AppSettings.LayerAssignment.builder()
                    .enabled(externalDataProperties.isLayerEnabled(layer))
                    .primaryProvider(ExternalProviderIds.SOCCER365)
                    .build();
            case ODDS -> AppSettings.LayerAssignment.builder()
                    .enabled(externalDataProperties.isLayerEnabled(layer))
                    .primaryProvider(ExternalProviderIds.MARATHONBET)
                    .build();
            case LIVE -> AppSettings.LayerAssignment.builder()
                    .enabled(externalDataProperties.isLayerEnabled(layer))
                    .primaryProvider(ExternalProviderIds.TWENTYFOUR_SCORE)
                    .build();
        };
    }

    /**
     * Static fallback when Spring context is unavailable (legacy migration).
     * Matches {@code external-data.layers.*.enabled=true} defaults.
     */
    public static AppSettings.LayerAssignment staticDefaultLayerAssignment(ExternalDataLayer layer) {
        return switch (layer) {
            case SCHEDULE, FULL_MATCH -> AppSettings.LayerAssignment.builder()
                    .enabled(true)
                    .primaryProvider(ExternalProviderIds.SOCCER365)
                    .build();
            case ODDS -> AppSettings.LayerAssignment.builder()
                    .enabled(true)
                    .primaryProvider(ExternalProviderIds.MARATHONBET)
                    .build();
            case LIVE -> AppSettings.LayerAssignment.builder()
                    .enabled(true)
                    .primaryProvider(ExternalProviderIds.TWENTYFOUR_SCORE)
                    .build();
        };
    }

    public AppSettings.ExternalDataLayersBlock defaultExternalDataLayers() {
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            layers.put(layer, defaultLayerAssignment(layer));
        }
        return AppSettings.ExternalDataLayersBlock.builder().layers(layers).build();
    }

    /**
     * Persist {@code enabled=false} for a layer (circuit breaker). Returns true when the flag changed.
     * Kept on AppSettingsService so scrape clients do not depend on LayerProviderRegistry.
     */
    @Transactional
    public boolean disableLayer(ExternalDataLayer layer, String reason) {
        if (layer == null) {
            return false;
        }
        AppSettings settings = getOrCreate();
        AppSettings.ExternalDataLayersBlock block = settings.getExternalDataLayers();
        if (block == null || block.getLayers() == null) {
            block = defaultExternalDataLayers();
            settings.setExternalDataLayers(block);
        }
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        if (block.getLayers() != null) {
            layers.putAll(block.getLayers());
        }
        for (ExternalDataLayer l : ExternalDataLayer.values()) {
            layers.putIfAbsent(l, defaultLayerAssignment(l));
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
        settings.setExternalDataLayers(AppSettings.ExternalDataLayersBlock.builder().layers(layers).build());
        save(settings);
        return true;
    }

    public static AppSettings.ExternalDataLayersBlock staticDefaultExternalDataLayers() {
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            layers.put(layer, staticDefaultLayerAssignment(layer));
        }
        return AppSettings.ExternalDataLayersBlock.builder().layers(layers).build();
    }

    private AppSettings createDefaults() {
        return appSettingsRepository.save(AppSettings.builder()
                .id(AppSettings.DEFAULT_ID)
                .clientVersion(AppSettings.ClientVersionBlock.builder().build())
                .externalDataLayers(defaultExternalDataLayers())
                .build());
    }
}
