package net.friendly_bets.aiscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.aiscore.config.AiscoreProperties;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs only when aiscore.com is SCHEDULE primary.
 * Syncs all leagues of the running season that have a tournament path configured.
 */
@Component
@RequiredArgsConstructor
public class AiscoreScheduleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiscoreScheduleSyncScheduler.class);

    private final AiscoreProperties properties;
    private final AiscoreScheduleSyncService scheduleSyncService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalDataLayerConfigService layerConfigService;

    private final Map<String, Instant> lastSyncAt = new ConcurrentHashMap<>();
    private final Instant startedAt = Instant.now();

    @Scheduled(fixedDelayString = "${aiscore.scheduler-tick-ms:300000}")
    public void tick() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.SCHEDULE)) {
            return;
        }
        String primary = layerConfigService.assignment(ExternalDataLayer.SCHEDULE).getPrimaryProvider();
        if (!MatchDataProviders.AISCORE.equals(primary)) {
            return;
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return;
        }
        Season season = seasonOpt.get();
        List<League> leagues = syncableLeagues(season);
        Instant now = Instant.now();
        for (int i = 0; i < leagues.size(); i++) {
            League league = leagues.get(i);
            String leagueCode = league.getLeagueCode().name();
            if (!isDue(leagueCode, i, now)) {
                continue;
            }
            try {
                scheduleSyncService.syncLeague(season, league, true);
            } catch (Exception e) {
                log.warn("aiscore schedule sync failed for {}: {}", leagueCode, e.getMessage());
            } finally {
                lastSyncAt.put(leagueCode, Instant.now());
            }
        }
    }

    private List<League> syncableLeagues(Season season) {
        List<League> out = new ArrayList<>();
        Map<String, String> paths = properties.getTournamentPaths();
        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            String path = paths != null ? paths.get(league.getLeagueCode().name()) : null;
            if (path == null || path.isBlank()) {
                continue;
            }
            out.add(league);
        }
        return out;
    }

    private boolean isDue(String leagueCode, int leagueIndex, Instant now) {
        Instant last = lastSyncAt.get(leagueCode);
        long intervalMs = Math.max(60_000L, properties.getScheduleSyncIntervalMs());
        long staggerMs = Math.max(0L, properties.getLeagueStaggerMs()) * leagueIndex;
        int jitterMinutes = Math.max(0, properties.getSyncJitterMinutes());
        long jitterMs = jitterMinutes <= 0
                ? 0L
                : ThreadLocalRandom.current().nextLong(-jitterMinutes * 60_000L, jitterMinutes * 60_000L + 1);

        if (last == null) {
            Instant firstEligible = startedAt.plusMillis(staggerMs + Math.max(0L, jitterMs));
            return !now.isBefore(firstEligible);
        }
        Instant next = last.plusMillis(intervalMs + jitterMs);
        return !now.isBefore(next);
    }
}
