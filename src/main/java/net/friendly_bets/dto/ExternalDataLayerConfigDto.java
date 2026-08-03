package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalDataLayerConfigDto {

    private Map<ExternalDataLayer, LayerAssignmentDto> layers;
    private Map<String, List<String>> capabilities;
    /** ODDS layer: hours before kickoff for force-refresh / existing-odds window. */
    private Integer oddsRefreshWithinHours;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayerAssignmentDto {
        private Boolean enabled;
        private String primaryProvider;
        private String secondaryProvider;

        public static LayerAssignmentDto from(AppSettings.LayerAssignment a) {
            if (a == null) {
                return LayerAssignmentDto.builder().enabled(true).build();
            }
            return LayerAssignmentDto.builder()
                    .enabled(a.getEnabled() == null || a.getEnabled())
                    .primaryProvider(a.getPrimaryProvider())
                    .secondaryProvider(a.getSecondaryProvider())
                    .build();
        }

        public AppSettings.LayerAssignment toEntity() {
            return AppSettings.LayerAssignment.builder()
                    .enabled(enabled == null || enabled)
                    .primaryProvider(primaryProvider)
                    .secondaryProvider(secondaryProvider)
                    .build();
        }
    }

    public static ExternalDataLayerConfigDto from(
            AppSettings.ExternalDataLayersBlock config,
            Map<String, List<String>> capabilities
    ) {
        Map<ExternalDataLayer, LayerAssignmentDto> layers = new EnumMap<>(ExternalDataLayer.class);
        Integer oddsHours = null;
        if (config != null) {
            oddsHours = config.getOddsRefreshWithinHours();
            if (config.getLayers() != null) {
                config.getLayers().forEach((layer, assignment) ->
                        layers.put(layer, LayerAssignmentDto.from(assignment)));
            }
        }
        return ExternalDataLayerConfigDto.builder()
                .layers(layers)
                .capabilities(capabilities)
                .oddsRefreshWithinHours(oddsHours)
                .build();
    }

    public Map<ExternalDataLayer, AppSettings.LayerAssignment> toEntityLayers() {
        Map<ExternalDataLayer, AppSettings.LayerAssignment> map = new EnumMap<>(ExternalDataLayer.class);
        if (layers != null) {
            layers.forEach((layer, dto) -> map.put(layer, dto != null ? dto.toEntity() : null));
        }
        return map;
    }
}
