package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.StandingsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StandingsSyncOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(StandingsSyncOrchestrator.class);

    private final LayerProviderRouter router;
    private final ExternalDataLayerConfigService layerConfigService;

    public void syncLeagueAfterMatchFinalized(String leagueCode, String seasonId) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return;
        }
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.STANDINGS)) {
            return;
        }
        try {
            router.execute(
                    ExternalDataLayer.STANDINGS,
                    StandingsProvider.class,
                    p -> p.syncByLeagueCode(leagueCode.trim()),
                    leagueCode.trim()
            );
            log.info("STANDINGS synced for league {} season {}", leagueCode, seasonId);
        } catch (RuntimeException e) {
            log.warn("STANDINGS sync failed for league {}: {}", leagueCode, e.getMessage());
        }
    }
}
