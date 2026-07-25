package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.providers.ExternalDataLayerConfig;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRegistry;
import net.friendly_bets.repositories.ExternalDataLayerConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalDataLayerConfigService {

    private final ExternalDataLayerConfigRepository repository;
    private final LayerProviderRegistry registry;

    @Transactional
    public ExternalDataLayerConfig getOrCreateDefaults() {
        return repository.findById(ExternalDataLayerConfig.SINGLETON_ID)
                .orElseGet(this::createDefaults);
    }

    public ExternalDataLayerConfig.LayerAssignment assignment(ExternalDataLayer layer) {
        ExternalDataLayerConfig config = getOrCreateDefaults();
        ExternalDataLayerConfig.LayerAssignment assignment = config.getLayers() != null
                ? config.getLayers().get(layer)
                : null;
        if (assignment == null) {
            assignment = defaultAssignment(layer);
        }
        return assignment;
    }

    @Transactional
    public ExternalDataLayerConfig update(Map<ExternalDataLayer, ExternalDataLayerConfig.LayerAssignment> layers) {
        if (layers == null) {
            throw new BadRequestException("externalDataLayerConfigRequired");
        }
        ExternalDataLayerConfig config = getOrCreateDefaults();
        Map<ExternalDataLayer, ExternalDataLayerConfig.LayerAssignment> next = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            ExternalDataLayerConfig.LayerAssignment incoming = layers.get(layer);
            if (incoming == null) {
                next.put(layer, defaultAssignment(layer));
                continue;
            }
            validateAssignment(layer, incoming);
            next.put(layer, ExternalDataLayerConfig.LayerAssignment.builder()
                    .primaryProvider(blankToNull(incoming.getPrimaryProvider()))
                    .secondaryProvider(blankToNull(incoming.getSecondaryProvider()))
                    .build());
        }
        config.setLayers(next);
        return repository.save(config);
    }

    public Map<String, List<String>> capabilitiesCatalog() {
        Map<String, List<String>> catalog = new LinkedHashMap<>();
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            catalog.put(layer.name(), registry.providerIdsFor(layer));
        }
        return catalog;
    }

    private ExternalDataLayerConfig createDefaults() {
        Map<ExternalDataLayer, ExternalDataLayerConfig.LayerAssignment> layers = new EnumMap<>(ExternalDataLayer.class);
        for (ExternalDataLayer layer : ExternalDataLayer.values()) {
            layers.put(layer, defaultAssignment(layer));
        }
        ExternalDataLayerConfig created = ExternalDataLayerConfig.builder()
                .id(ExternalDataLayerConfig.SINGLETON_ID)
                .layers(layers)
                .build();
        return repository.save(created);
    }

    private ExternalDataLayerConfig.LayerAssignment defaultAssignment(ExternalDataLayer layer) {
        return switch (layer) {
            case SCHEDULE, FULL_MATCH -> ExternalDataLayerConfig.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.SOCCER365)
                    .build();
            case ODDS -> ExternalDataLayerConfig.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.MARATHONBET)
                    .build();
            case LIVE -> ExternalDataLayerConfig.LayerAssignment.builder()
                    .primaryProvider(MatchDataProviders.TWENTYFOUR_SCORE)
                    .build();
        };
    }

    private void validateAssignment(ExternalDataLayer layer, ExternalDataLayerConfig.LayerAssignment assignment) {
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
