package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalDataLayersUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Persists circuit-breaker disable / auto-re-enable for a layer without depending on
 * {@link net.friendly_bets.providers.LayerProviderRegistry}.
 * Avoids a Spring bean cycle:
 * ConfigService → Registry → Providers → HttpClients → CircuitBreaker → ConfigService.
 */
@Service
@RequiredArgsConstructor
public class ExternalDataLayerAutoDisableService {

    private final AppSettingsService appSettingsService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Persist {@code enabled=false} + {@code circuit_disabled_at} (circuit breaker).
     * Returns true when the enabled flag changed from on→off.
     * If already off without a stamp, stamps {@code circuit_disabled_at} so cooldown can fire.
     */
    @Transactional
    public boolean disableLayer(ExternalDataLayer layer, String reason) {
        if (layer == null) {
            return false;
        }
        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ExternalDataLayersBlock block = ensureBlock(settings);
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = copyLayers(block);
        AppSettings.LayerAssignment current = layers.get(layer);
        Instant now = Instant.now();

        if (current != null && Boolean.FALSE.equals(current.getEnabled())) {
            if (current.getCircuitDisabledAt() != null) {
                return false;
            }
            layers.put(layer, copyAssignment(current, false, now));
            persist(settings, block, layers);
            return false;
        }

        layers.put(layer, AppSettings.LayerAssignment.builder()
                .enabled(false)
                .primaryProvider(current != null ? current.getPrimaryProvider() : null)
                .secondaryProvider(current != null ? current.getSecondaryProvider() : null)
                .circuitDisabledAt(now)
                .build());
        persist(settings, block, layers);
        return true;
    }

    /**
     * Re-enable a circuit-disabled layer after {@code cooldown} from {@code circuit_disabled_at}.
     * No-op for admin-only disables (no stamp) or while cooldown is still active.
     */
    @Transactional
    public boolean reenableIfCircuitCooldownElapsed(ExternalDataLayer layer, Duration cooldown) {
        if (layer == null || cooldown == null || cooldown.isNegative() || cooldown.isZero()) {
            return false;
        }
        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ExternalDataLayersBlock block = ensureBlock(settings);
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = copyLayers(block);
        AppSettings.LayerAssignment current = layers.get(layer);
        if (current == null || current.getCircuitDisabledAt() == null) {
            return false;
        }
        if (!Boolean.FALSE.equals(current.getEnabled())) {
            layers.put(layer, copyAssignment(current, current.getEnabled(), null));
            persist(settings, block, layers);
            return false;
        }
        Instant readyAt = current.getCircuitDisabledAt().plus(cooldown);
        if (Instant.now().isBefore(readyAt)) {
            return false;
        }
        layers.put(layer, copyAssignment(current, true, null));
        persist(settings, block, layers);
        return true;
    }

    /** True while enabled=false and circuit stamp is still within cooldown. */
    public boolean isCircuitCooldownActive(ExternalDataLayer layer, Duration cooldown) {
        if (layer == null || cooldown == null || cooldown.isNegative() || cooldown.isZero()) {
            return false;
        }
        AppSettings.LayerAssignment assignment = assignmentOf(layer);
        if (assignment == null
                || assignment.getCircuitDisabledAt() == null
                || !Boolean.FALSE.equals(assignment.getEnabled())) {
            return false;
        }
        return Instant.now().isBefore(assignment.getCircuitDisabledAt().plus(cooldown));
    }

    private AppSettings.LayerAssignment assignmentOf(ExternalDataLayer layer) {
        AppSettings settings = appSettingsService.getOrCreate();
        AppSettings.ExternalDataLayersBlock block = settings.getExternalDataLayers();
        if (block == null || block.getLayers() == null) {
            return null;
        }
        return block.getLayers().get(layer);
    }

    private AppSettings.ExternalDataLayersBlock ensureBlock(AppSettings settings) {
        AppSettings.ExternalDataLayersBlock block = settings.getExternalDataLayers();
        if (block == null || block.getLayers() == null) {
            block = appSettingsService.defaultExternalDataLayers();
            settings.setExternalDataLayers(block);
        }
        return block;
    }

    private Map<ExternalDataLayer, AppSettings.LayerAssignment> copyLayers(
            AppSettings.ExternalDataLayersBlock block
    ) {
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        if (block.getLayers() != null) {
            layers.putAll(block.getLayers());
        }
        for (ExternalDataLayer l : ExternalDataLayer.values()) {
            layers.putIfAbsent(l, appSettingsService.defaultLayerAssignment(l));
        }
        return layers;
    }

    private void persist(
            AppSettings settings,
            AppSettings.ExternalDataLayersBlock block,
            Map<ExternalDataLayer, AppSettings.LayerAssignment> layers
    ) {
        settings.setExternalDataLayers(AppSettings.ExternalDataLayersBlock.builder()
                .layers(layers)
                .oddsRefreshWithinHours(block.getOddsRefreshWithinHours())
                .build());
        appSettingsService.save(settings);
        eventPublisher.publishEvent(new ExternalDataLayersUpdatedEvent());
    }

    private static AppSettings.LayerAssignment copyAssignment(
            AppSettings.LayerAssignment current,
            Boolean enabled,
            Instant circuitDisabledAt
    ) {
        return AppSettings.LayerAssignment.builder()
                .enabled(enabled)
                .primaryProvider(current.getPrimaryProvider())
                .secondaryProvider(current.getSecondaryProvider())
                .circuitDisabledAt(circuitDisabledAt)
                .build();
    }
}
