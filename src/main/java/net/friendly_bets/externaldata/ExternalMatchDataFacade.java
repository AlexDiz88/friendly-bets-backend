package net.friendly_bets.externaldata;

import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.client.dto.FootballDataMatchDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Resolves slot → external API query and fetches matches, with failover across providers.
 */
@Service
@Slf4j
public class ExternalMatchDataFacade {

    private final List<ExternalMatchDataProvider> providers;

    public ExternalMatchDataFacade(List<ExternalMatchDataProvider> providerList) {
        this.providers = providerList.stream()
                .sorted(Comparator.comparingInt(ExternalMatchDataProvider::priority))
                .toList();
    }

    public List<FootballDataMatchDto> fetchMatches(ExternalMatchFetchRequest request) {
        if (providers.isEmpty()) {
            throw new BadRequestException("noExternalMatchDataProvider");
        }

        RuntimeException lastFailure = null;
        for (ExternalMatchDataProvider provider : providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            try {
                return provider.fetchMatches(request);
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("External match fetch failed via {}: {}", provider.providerId(), e.getMessage());
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new BadRequestException("footballDataApiKeyNotConfigured");
    }
}
