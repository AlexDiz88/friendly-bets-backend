package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.LiveMatchProvider;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.MatchFinalizeOrchestrator;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TwentyFourScoreLiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreLiveScheduler.class);

    private final RunningSeasonLookup runningSeasonLookup;
    private final LayerProviderRouter router;
    private final MatchFinalizeOrchestrator matchFinalizeOrchestrator;
    private final ExternalDataLayerConfigService layerConfigService;

    @Scheduled(fixedDelayString = "${twentyfourscore.scheduler-tick-ms:300000}")
    public void tick() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.LIVE)) {
            return;
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return;
        }
        Season season = seasonOpt.get();
        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            if (!TwentyFourScoreLeagueTitles.supported().contains(league.getLeagueCode())) {
                continue;
            }
            try {
                LiveMatchProvider.LiveSyncResult result = router.execute(
                        ExternalDataLayer.LIVE,
                        LiveMatchProvider.class,
                        p -> p.syncLeagueLive(season, league),
                        league.getLeagueCode().name()
                );
                if (result.updated() > 0 || result.finishedDetected() > 0
                        || !result.pendingFullMatchIds().isEmpty()) {
                    log.info("24score LIVE {} updated={} finished={} pendingFull={}",
                            result.leagueCode(), result.updated(), result.finishedDetected(),
                            result.pendingFullMatchIds().size());
                }
                if (layerConfigService.isLayerEnabled(ExternalDataLayer.FULL_MATCH)) {
                    try {
                        matchFinalizeOrchestrator.finalizePendingFullMatches(result.pendingFullMatchIds());
                    } catch (RuntimeException fullEx) {
                        log.warn("FULL after LIVE failed for {}: {}", league.getLeagueCode(), fullEx.getMessage());
                    }
                }
            } catch (RuntimeException e) {
                log.warn("24score LIVE sync failed for {}: {}", league.getLeagueCode(), e.getMessage());
            }
        }
    }
}
