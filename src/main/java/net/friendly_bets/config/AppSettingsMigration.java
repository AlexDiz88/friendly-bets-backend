package net.friendly_bets.config;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.AppSettingsRepository;
import net.friendly_bets.services.AppSettingsService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * Merges legacy singleton collections {@code client_version} and {@code external_data_layer_config}
 * into {@code app_settings} ({@code _id: default}), then drops the old collections.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class AppSettingsMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppSettingsMigration.class);
    private static final String LEGACY_CLIENT_VERSION = "client_version";
    private static final String LEGACY_LAYER_CONFIG = "external_data_layer_config";

    private final AppSettingsRepository appSettingsRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        boolean hasLegacyClient = mongoTemplate.collectionExists(LEGACY_CLIENT_VERSION);
        boolean hasLegacyLayers = mongoTemplate.collectionExists(LEGACY_LAYER_CONFIG);

        if (hasLegacyClient || hasLegacyLayers) {
            AppSettings settings = appSettingsRepository.findById(AppSettings.DEFAULT_ID)
                    .orElseGet(() -> AppSettings.builder()
                            .id(AppSettings.DEFAULT_ID)
                            .clientVersion(AppSettings.ClientVersionBlock.builder().build())
                            .externalDataLayers(AppSettingsService.staticDefaultExternalDataLayers())
                            .build());

            AppSettings.ClientVersionBlock legacyClient = readLegacyClientVersion();
            if (legacyClient != null && legacyClient.getBuildId() != null) {
                settings.setClientVersion(legacyClient);
            }

            AppSettings.ExternalDataLayersBlock legacyLayers = readLegacyLayers();
            if (legacyLayers != null) {
                settings.setExternalDataLayers(legacyLayers);
            } else if (isBlankLayers(settings.getExternalDataLayers())) {
                settings.setExternalDataLayers(AppSettingsService.staticDefaultExternalDataLayers());
            }

            appSettingsRepository.save(settings);
            log.info("Migrated client_version + external_data_layer_config into app_settings");
        }

        if (hasLegacyClient) {
            mongoTemplate.dropCollection(LEGACY_CLIENT_VERSION);
            log.info("Dropped legacy collection {}", LEGACY_CLIENT_VERSION);
        }
        if (hasLegacyLayers) {
            mongoTemplate.dropCollection(LEGACY_LAYER_CONFIG);
            log.info("Dropped legacy collection {}", LEGACY_LAYER_CONFIG);
        }
    }

    private static boolean isBlankLayers(AppSettings.ExternalDataLayersBlock block) {
        return block == null || block.getLayers() == null || block.getLayers().isEmpty();
    }

    private AppSettings.ClientVersionBlock readLegacyClientVersion() {
        if (!mongoTemplate.collectionExists(LEGACY_CLIENT_VERSION)) {
            return null;
        }
        Document doc = mongoTemplate.findById("current", Document.class, LEGACY_CLIENT_VERSION);
        if (doc == null) {
            return null;
        }
        return AppSettings.ClientVersionBlock.builder()
                .buildId(doc.getString("build_id"))
                .updatedAt(toLocalDateTime(doc.get("updated_at")))
                .build();
    }

    private AppSettings.ExternalDataLayersBlock readLegacyLayers() {
        if (!mongoTemplate.collectionExists(LEGACY_LAYER_CONFIG)) {
            return null;
        }
        Document doc = mongoTemplate.findById("default", Document.class, LEGACY_LAYER_CONFIG);
        if (doc == null) {
            return null;
        }
        Document layersDoc = doc.get("layers", Document.class);
        if (layersDoc == null || layersDoc.isEmpty()) {
            return null;
        }
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            Document assignment = layersDoc.get(layer.name(), Document.class);
            if (assignment == null) {
                layers.put(layer, AppSettingsService.staticDefaultLayerAssignment(layer));
                continue;
            }
            Boolean enabled = assignment.getBoolean("enabled");
            layers.put(layer, AppSettings.LayerAssignment.builder()
                    .enabled(enabled == null || enabled)
                    .primaryProvider(assignment.getString("primary_provider"))
                    .secondaryProvider(assignment.getString("secondary_provider"))
                    .build());
        }
        return AppSettings.ExternalDataLayersBlock.builder().layers(layers).build();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }
}
