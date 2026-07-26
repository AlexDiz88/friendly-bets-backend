package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRegistry;
import net.friendly_bets.providers.config.ExternalDataProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalDataLayerConfigService {

    private final AppSettingsService appSettingsService;
    private final LayerProviderRegistry registry;
    private final ExternalDataProperties externalDataProperties;

    @Transactional
    public AppSettings.ExternalDataLayersBlock getOrCreateDefaults() {
        AppSettings settings = appSettingsService.getOrCreate();
        if (settings.getExternalDataLayers() == null) {
            settings.setExternalDataLayers(appSettingsService.defaultExternalDataLayers());
            appSettingsService.save(settings);
        }
        return settings.getExternalDataLayers();
    }

    public AppSettings.LayerAssignment assignment(ExternalDataLayer layer) {
        AppSettings.ExternalDataLayersBlock config = getOrCreateDefaults();
        AppSettings.LayerAssignment assignment = config.getLayers() != null
                ? config.getLayers().get(layer)
                : null;
        if (assignment == null) {
            assignment = appSettingsService.defaultLayerAssignment(layer);
        }
        return assignment;
    }

    /** Auto-sync gate for schedulers. Mongo override, else application.properties defaults. */
    public boolean isLayerEnabled(ExternalDataLayer layer) {
        AppSettings.LayerAssignment assignment = assignment(layer);
        if (assignment.getEnabled() != null) {
            return assignment.getEnabled();
        }
        return externalDataProperties.isLayerEnabled(layer);
    }

    @Transactional
    public AppSettings.ExternalDataLayersBlock update(Map<ExternalDataLayer, AppSettings.LayerAssignment> layers) {
        if (layers == null) {
            throw new BadRequestException("externalDataLayerConfigRequired");
        }
        AppSettings settings = appSettingsService.getOrCreate();
        Map<ExternalDataLayer, AppSettings.LayerAssignment> next = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            AppSettings.LayerAssignment incoming = layers.get(layer);
            if (incoming == null) {
                next.put(layer, appSettingsService.defaultLayerAssignment(layer));
                continue;
            }
            validateAssignment(layer, incoming);
            next.put(layer, AppSettings.LayerAssignment.builder()
                    .enabled(incoming.getEnabled() == null || incoming.getEnabled())
                    .primaryProvider(blankToNull(incoming.getPrimaryProvider()))
                    .secondaryProvider(blankToNull(incoming.getSecondaryProvider()))
                    .build());
        }
        AppSettings.ExternalDataLayersBlock block = AppSettings.ExternalDataLayersBlock.builder()
                .layers(next)
                .build();
        settings.setExternalDataLayers(block);
        appSettingsService.save(settings);
        return block;
    }

    public Map<String, List<String>> capabilitiesCatalog() {
        Map<String, List<String>> catalog = new LinkedHashMap<>();
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            catalog.put(layer.name(), registry.providerIdsFor(layer));
        }
        return catalog;
    }

    private void validateAssignment(ExternalDataLayer layer, AppSettings.LayerAssignment assignment) {
        String primary = blankToNull(assignment.getPrimaryProvider());
        String secondary = blankToNull(assignment.getSecondaryProvider());
        if (primary != null) {
            requireSupports(layer, primary);
        }
        if (secondary != null) {
            requireSupports(layer, secondary);
        }
        if (primary != null && primary.equals(secondary)) {
            throw new BadRequestException("externalDataLayerSecondarySameAsPrimary");
        }
    }

    private void requireSupports(ExternalDataLayer layer, String providerId) {
        boolean ok = registry.providersFor(layer).stream()
                .anyMatch(p -> providerId.equals(p.providerId()));
        if (!ok) {
            throw new BadRequestException("externalDataProviderUnavailable");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
