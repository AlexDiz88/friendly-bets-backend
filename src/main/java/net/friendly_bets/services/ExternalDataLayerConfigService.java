package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataLayersUpdatedEvent;
import net.friendly_bets.providers.LayerProviderRegistry;
import net.friendly_bets.providers.config.ExternalDataProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalDataLayerConfigService {

    public static final int ODDS_REFRESH_WITHIN_HOURS_MIN = 1;
    public static final int ODDS_REFRESH_WITHIN_HOURS_MAX = 168;

    private final AppSettingsService appSettingsService;
    private final LayerProviderRegistry registry;
    private final ExternalDataProperties externalDataProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AppSettings.ExternalDataLayersBlock getOrCreateDefaults() {
        AppSettings settings = appSettingsService.getOrCreate();
        boolean dirty = false;
        if (settings.getExternalDataLayers() == null) {
            settings.setExternalDataLayers(appSettingsService.defaultExternalDataLayers());
            dirty = true;
        } else if (settings.getExternalDataLayers().getOddsRefreshWithinHours() == null
                || settings.getExternalDataLayers().getOddsRefreshWithinHours() <= 0) {
            settings.getExternalDataLayers().setOddsRefreshWithinHours(
                    externalDataProperties.oddsRefreshWithinHours());
            dirty = true;
        }
        if (dirty) {
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

    /**
     * Persist {@code enabled=true} when the layer is currently off (circuit / admin).
     * No-op if already enabled. Returns true when the flag changed.
     */
    @Transactional
    public boolean ensureLayerEnabled(ExternalDataLayer layer) {
        if (layer == null || isLayerEnabled(layer)) {
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
        AppSettings.LayerAssignment next = AppSettings.LayerAssignment.builder()
                .enabled(true)
                .primaryProvider(current != null ? current.getPrimaryProvider() : null)
                .secondaryProvider(current != null ? current.getSecondaryProvider() : null)
                .circuitDisabledAt(null)
                .build();
        layers.put(layer, next);
        settings.setExternalDataLayers(AppSettings.ExternalDataLayersBlock.builder()
                .layers(layers)
                .oddsRefreshWithinHours(block.getOddsRefreshWithinHours())
                .build());
        appSettingsService.save(settings);
        eventPublisher.publishEvent(new ExternalDataLayersUpdatedEvent());
        return true;
    }

    /** ODDS cron refresh / near-kickoff window (hours). Mongo, else properties default. */
    public int oddsRefreshWithinHours() {
        return appSettingsService.oddsRefreshWithinHours();
    }

    @Transactional
    public AppSettings.ExternalDataLayersBlock update(
            Map<ExternalDataLayer, AppSettings.LayerAssignment> layers,
            Integer oddsRefreshWithinHours
    ) {
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
            // Admin PATCH owns enabled state — clear circuit stamp (no auto-re-enable).
            next.put(layer, AppSettings.LayerAssignment.builder()
                    .enabled(incoming.getEnabled() == null || incoming.getEnabled())
                    .primaryProvider(blankToNull(incoming.getPrimaryProvider()))
                    .secondaryProvider(blankToNull(incoming.getSecondaryProvider()))
                    .circuitDisabledAt(null)
                    .build());
        }

        Integer resolvedHours = resolveOddsRefreshWithinHours(
                oddsRefreshWithinHours,
                settings.getExternalDataLayers()
        );

        AppSettings.ExternalDataLayersBlock block = AppSettings.ExternalDataLayersBlock.builder()
                .layers(next)
                .oddsRefreshWithinHours(resolvedHours)
                .build();
        settings.setExternalDataLayers(block);
        appSettingsService.save(settings);
        eventPublisher.publishEvent(new ExternalDataLayersUpdatedEvent());
        return block;
    }

    public Map<String, List<String>> capabilitiesCatalog() {
        Map<String, List<String>> catalog = new LinkedHashMap<>();
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            catalog.put(layer.name(), registry.providerIdsFor(layer));
        }
        return catalog;
    }

    private Integer resolveOddsRefreshWithinHours(
            Integer incoming,
            AppSettings.ExternalDataLayersBlock existing
    ) {
        if (incoming != null) {
            if (incoming < ODDS_REFRESH_WITHIN_HOURS_MIN || incoming > ODDS_REFRESH_WITHIN_HOURS_MAX) {
                throw new BadRequestException("oddsRefreshWithinHoursInvalid");
            }
            return incoming;
        }
        if (existing != null
                && existing.getOddsRefreshWithinHours() != null
                && existing.getOddsRefreshWithinHours() > 0) {
            return existing.getOddsRefreshWithinHours();
        }
        return externalDataProperties.oddsRefreshWithinHours();
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
