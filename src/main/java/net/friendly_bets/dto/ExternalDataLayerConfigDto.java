package net.friendly_bets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.providers.ExternalDataLayerConfig;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayerAssignmentDto {
        private String primaryProvider;
        private String secondaryProvider;

        public static LayerAssignmentDto from(ExternalDataLayerConfig.LayerAssignment a) {
            if (a == null) {
                return LayerAssignmentDto.builder().build();
            }
            return LayerAssignmentDto.builder()
                    .primaryProvider(a.getPrimaryProvider())
                    .secondaryProvider(a.getSecondaryProvider())
                    .build();
        }

        public ExternalDataLayerConfig.LayerAssignment toEntity() {
            return ExternalDataLayerConfig.LayerAssignment.builder()
                    .primaryProvider(primaryProvider)
                    .secondaryProvider(secondaryProvider)
                    .build();
        }
    }

    public static ExternalDataLayerConfigDto from(
            ExternalDataLayerConfig config,
            Map<String, List<String>> capabilities
    ) {
        Map<ExternalDataLayer, LayerAssignmentDto> layers = new EnumMap<>(ExternalDataLayer.class);
        if (config.getLayers() != null) {
            config.getLayers().forEach((layer, assignment) ->
                    layers.put(layer, LayerAssignmentDto.from(assignment)));
        }
        return ExternalDataLayerConfigDto.builder()
                .layers(layers)
                .capabilities(capabilities)
                .build();
    }

    public Map<ExternalDataLayer, ExternalDataLayerConfig.LayerAssignment> toEntityLayers() {
        Map<ExternalDataLayer, ExternalDataLayerConfig.LayerAssignment> map = new EnumMap<>(ExternalDataLayer.class);
        if (layers != null) {
            layers.forEach((layer, dto) -> map.put(layer, dto != null ? dto.toEntity() : null));
        }
        return map;
    }
}
