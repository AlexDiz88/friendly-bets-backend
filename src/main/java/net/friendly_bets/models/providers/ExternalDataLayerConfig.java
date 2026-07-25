package net.friendly_bets.models.providers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.EnumMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "external_data_layer_config")
public class ExternalDataLayerConfig {

    public static final String SINGLETON_ID = "default";

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "layers")
    @Builder.Default
    private Map<ExternalDataLayer, LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class LayerAssignment {
        @Field(name = "primary_provider")
        private String primaryProvider;

        @Field(name = "secondary_provider")
        private String secondaryProvider;
    }
}
