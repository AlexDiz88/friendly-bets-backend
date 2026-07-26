package net.friendly_bets.providers.config;

import lombok.Data;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

/**
 * Defaults for external-data layers (boot / seed). Runtime toggles live in {@code app_settings}.
 */
@Data
@ConfigurationProperties(prefix = "external-data")
public class ExternalDataProperties {

    private Map<ExternalDataLayer, LayerFlags> layers = defaultLayers();

    @Data
    public static class LayerFlags {
        /** Auto-sync for the layer (schedulers). Default true. */
        private boolean enabled = true;
    }

    public boolean isLayerEnabled(ExternalDataLayer layer) {
        if (layer == null || layers == null) {
            return true;
        }
        LayerFlags flags = layers.get(layer);
        if (flags == null) {
            return true;
        }
        return flags.isEnabled();
    }

    private static Map<ExternalDataLayer, LayerFlags> defaultLayers() {
        Map<ExternalDataLayer, LayerFlags> map = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            LayerFlags flags = new LayerFlags();
            flags.setEnabled(true);
            map.put(layer, flags);
        }
        return map;
    }
}
