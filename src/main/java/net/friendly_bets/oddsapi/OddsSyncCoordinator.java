package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.MarathonbetSyncResult;
import net.friendly_bets.marathonbet.MarathonbetSyncService;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled odds: Marathonbet primary for WC, odds-api as fallback.
 */
@Component
@RequiredArgsConstructor
public class OddsSyncCoordinator {

    private static final Logger log = LoggerFactory.getLogger(OddsSyncCoordinator.class);

    private final MarathonbetProperties marathonbetProperties;
    private final MarathonbetSyncService marathonbetSyncService;
    private final OddsApiSyncService oddsApiSyncService;
    private final OddsApiProperties oddsApiProperties;

    public void runScheduledSync() {
        MarathonbetSyncResult marathonResult = null;
        if (marathonbetProperties.isSyncEnabled()) {
            try {
                marathonResult = marathonbetSyncService.runTick();
            } catch (Exception e) {
                log.warn("marathonbet sync tick failed: {}", e.getMessage());
            }
        }

        if (marathonResult != null
                && marathonbetProperties.isFallbackToOddsApi()
                && !marathonResult.isTournamentFetched()) {
            oddsApiSyncService.runTick();
            return;
        }

        if (marathonResult != null
                && marathonbetProperties.isFallbackToOddsApi()
                && marathonResult.getFailedGameResultIds() != null
                && !marathonResult.getFailedGameResultIds().isEmpty()
                && marathonResult.getLeagueCode() != null
                && marathonResult.getSeason() != null) {
            fallbackFailedMatches(marathonResult);
        }

        boolean skipWcOddsApi = marathonResult != null
                && marathonResult.shouldSkipOddsApiForWc(marathonbetProperties.getSkipOddsApiMinMatchRatio());
        if (skipWcOddsApi && oddsApiProperties.isFallbackOnlyForWc()) {
            log.info("odds-api tick: skipping WC (marathonbet match ratio {}%)",
                    Math.round(marathonResult.matchRatio() * 100));
            oddsApiSyncService.runTickExcludingLeagues(List.of(League.LeagueCode.WC.name()));
            return;
        }

        if (marathonResult == null || !marathonbetProperties.isFallbackToOddsApi()) {
            oddsApiSyncService.runTick();
            return;
        }

        if (!skipWcOddsApi) {
            oddsApiSyncService.runTick();
        } else {
            oddsApiSyncService.runTickExcludingLeagues(List.of(League.LeagueCode.WC.name()));
        }
    }

    private void fallbackFailedMatches(MarathonbetSyncResult marathonResult) {
        if (marathonResult.getSlotOrders() == null || marathonResult.getSlotOrders().isEmpty()) {
            return;
        }
        for (int matchday : marathonResult.getSlotOrders()) {
            try {
                oddsApiSyncService.syncMatchdayByLeagueCode(
                        marathonResult.getLeagueCode(),
                        matchday,
                        marathonResult.getSeason(),
                        marathonResult.getFailedGameResultIds()
                );
            } catch (Exception e) {
                log.warn("odds-api fallback for marathon failures matchday={}: {}", matchday, e.getMessage());
            }
        }
    }
}
