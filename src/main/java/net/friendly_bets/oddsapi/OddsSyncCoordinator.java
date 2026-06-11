package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.MarathonbetSlotScope;
import net.friendly_bets.marathonbet.MarathonbetSyncResult;
import net.friendly_bets.marathonbet.MarathonbetSyncService;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled odds: Marathonbet primary for WC (SSE per match), odds-api только как fallback.
 */
@Component
@RequiredArgsConstructor
public class OddsSyncCoordinator {

    private static final Logger log = LoggerFactory.getLogger(OddsSyncCoordinator.class);

    private final MarathonbetProperties marathonbetProperties;
    private final MarathonbetSyncService marathonbetSyncService;
    private final OddsApiSyncService oddsApiSyncService;

    public void runMarathonSlotSync(MarathonbetSlotScope scope) {
        if (!marathonbetProperties.isSyncEnabled()) {
            return;
        }
        MarathonbetSyncResult marathonResult;
        try {
            marathonResult = marathonbetSyncService.runTick(scope);
        } catch (Exception e) {
            log.warn("marathonbet sync tick failed (scope={}): {}", scope, e.getMessage());
            return;
        }
        applyMarathonOddsApiFallback(marathonResult);
    }

    public void runScheduledSync() {
        if (!marathonbetProperties.isFallbackToOddsApi()) {
            oddsApiSyncService.runTick();
            return;
        }

        if (marathonbetProperties.isSyncEnabled()) {
            log.info("odds-api tick: excluding WC (marathonbet primary enabled)");
            oddsApiSyncService.runTickExcludingLeagues(List.of(League.LeagueCode.WC.name()));
            return;
        }

        oddsApiSyncService.runTick();
    }

    private void applyMarathonOddsApiFallback(MarathonbetSyncResult marathonResult) {
        if (!marathonbetProperties.isFallbackToOddsApi()) {
            return;
        }
        if (marathonResult == null || !marathonResult.isTournamentFetched()) {
            log.info("odds-api fallback: full tick (marathonbet tournament fetch failed)");
            oddsApiSyncService.runTick();
            return;
        }
        if (marathonResult.getFailedGameResultIds() != null
                && !marathonResult.getFailedGameResultIds().isEmpty()
                && marathonResult.getLeagueCode() != null
                && marathonResult.getSeason() != null) {
            fallbackFailedMatches(marathonResult);
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
