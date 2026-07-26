package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.AppSettings;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.repositories.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository appSettingsRepository;

    @Transactional
    public AppSettings getOrCreate() {
        return appSettingsRepository.findById(AppSettings.DEFAULT_ID)
                .orElseGet(this::createDefaults);
    }

    @Transactional
    public AppSettings save(AppSettings settings) {
        return appSettingsRepository.save(settings);
    }

    public static AppSettings.LayerAssignment defaultLayerAssignment(ExternalDataLayer layer) {
        return switch (layer) {
            case SCHEDULE, FULL_MATCH -> AppSettings.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.SOCCER365)
                    .build();
            case ODDS -> AppSettings.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.MARATHONBET)
                    .build();
            case LIVE -> AppSettings.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                    .build();
        };
    }

    public static AppSettings.ExternalDataLayersBlock defaultExternalDataLayers() {
        Map<ExternalDataLayer, AppSettings.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            layers.put(layer, defaultLayerAssignment(layer));
        }
        return AppSettings.ExternalDataLayersBlock.builder().layers(layers).build();
    }

    private AppSettings createDefaults() {
        return appSettingsRepository.save(AppSettings.builder()
                .id(AppSettings.DEFAULT_ID)
                .clientVersion(AppSettings.ClientVersionBlock.builder().build())
                .externalDataLayers(defaultExternalDataLayers())
                .build());
    }
}
