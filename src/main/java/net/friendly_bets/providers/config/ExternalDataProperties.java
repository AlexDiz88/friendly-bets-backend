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
    /** Default for {@code app_settings.external_data_layers.odds_refresh_within_hours}. */
    private OddsDefaults odds = new OddsDefaults();

    @Data
    public static class LayerFlags {
        /** Auto-sync for the layer (schedulers). Default true. */
        private boolean enabled = true;
    }

    @Data
    public static class OddsDefaults {
        /** Hours before kickoff for ODDS force-refresh / existing-odds refresh window. */
        private int refreshWithinHours = 36;
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

    public int oddsRefreshWithinHours() {
        if (odds == null || odds.getRefreshWithinHours() <= 0) {
            return 36;
        }
        return odds.getRefreshWithinHours();
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
