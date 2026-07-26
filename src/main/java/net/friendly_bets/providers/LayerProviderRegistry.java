package net.friendly_bets.providers;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LayerProviderRegistry {

    private final Map<String, List<ExternalDataProvider>> byId = new HashMap<>();
    private final Map<ExternalDataLayer, List<ExternalDataProvider>> byLayer = new EnumMap<>(ExternalDataLayer.class);

    public LayerProviderRegistry(List<ExternalDataProvider> providers) {
        for (ExternalDataProvider provider : providers) {
            if (provider == null || provider.providerId() == null) {
                continue;
            }
            byId.computeIfAbsent(provider.providerId(), k -> new ArrayList<>()).add(provider);
            for (ExternalDataLayer layer : provider.capabilities()) {
                byLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(provider);
            }
        }
    }

    public Optional<ExternalDataProvider> find(String providerId) {
        List<ExternalDataProvider> list = byId.get(providerId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    public <T extends ExternalDataProvider> Optional<T> findAs(String providerId, Class<T> type) {
        List<ExternalDataProvider> list = byId.get(providerId);
        if (list == null) {
            return Optional.empty();
        }
        return list.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    public List<ExternalDataProvider> providersFor(ExternalDataLayer layer) {
        return byLayer.getOrDefault(layer, List.of());
    }

    public Collection<ExternalDataProvider> all() {
        return byId.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public List<String> providerIdsFor(ExternalDataLayer layer) {
        Set<String> ids = new LinkedHashSet<>();
        for (ExternalDataProvider p : providersFor(layer)) {
            ids.add(p.providerId());
        }
        return new ArrayList<>(ids).stream().sorted().collect(Collectors.toList());
    }
}
