package net.friendly_bets.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "app_settings")
public class AppSettings {

    public static final String DEFAULT_ID = "default";

    @MongoId
    @Field(name = "_id")
    private String id;

    @Field(name = "client_version")
    private ClientVersionBlock clientVersion;

    @Field(name = "external_data_layers")
    private ExternalDataLayersBlock externalDataLayers;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class ClientVersionBlock {
        @Field(name = "build_id")
        private String buildId;

        @Field(name = "updated_at")
        private LocalDateTime updatedAt;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class ExternalDataLayersBlock {
        @Field(name = "layers")
        @Builder.Default
        private Map<ExternalDataLayer, LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class LayerAssignment {
        /** Auto-sync for the layer. Null = treat as enabled (legacy docs). */
        @Field(name = "enabled")
        private Boolean enabled;

        @Field(name = "primary_provider")
        private String primaryProvider;

        @Field(name = "secondary_provider")
        private String secondaryProvider;
    }
}
