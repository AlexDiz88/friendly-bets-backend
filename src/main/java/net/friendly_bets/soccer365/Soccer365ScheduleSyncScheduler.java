package net.friendly_bets.soccer365;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.soccer365.config.Soccer365Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Staggered schedule sync: ~8h per league, league offsets via {@code league-stagger-ms}, jitter ±N min.
 */
@Component
@RequiredArgsConstructor
public class Soccer365ScheduleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(Soccer365ScheduleSyncScheduler.class);

    private final Soccer365Properties properties;
    private final Soccer365ScheduleSyncService scheduleSyncService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalDataLayerConfigService layerConfigService;

    private final Map<String, Instant> lastSyncAt = new ConcurrentHashMap<>();
    private final Instant startedAt = Instant.now();

    @Scheduled(fixedDelayString = "${soccer365.scheduler-tick-ms:300000}")
    public void tick() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.SCHEDULE)) {
            return;
        }
        String primary = layerConfigService.assignment(ExternalDataLayer.SCHEDULE).getPrimaryProvider();
        if (!MatchDataProviders.SOCCER365.equals(primary)) {
            return;
        }
        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return;
        }
        Season season = seasonOpt.get();
        List<String> enabled = properties.getEnabledLeagues();
        if (enabled == null || enabled.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (int i = 0; i < enabled.size(); i++) {
            String code = enabled.get(i);
            if (code == null || code.isBlank()) {
                continue;
            }
            String leagueCode = code.trim().toUpperCase();
            if (!isDue(leagueCode, i, now)) {
                continue;
            }
            League league = season.getLeagues().stream()
                    .filter(l -> l != null && l.getLeagueCode() != null && leagueCode.equals(l.getLeagueCode().name()))
                    .findFirst()
                    .orElse(null);
            if (league == null) {
                continue;
            }
            try {
                scheduleSyncService.syncLeague(season, league, true);
            } catch (Exception e) {
                log.warn("soccer365 schedule sync failed for {}: {}", leagueCode, e.getMessage());
            } finally {
                lastSyncAt.put(leagueCode, Instant.now());
            }
        }
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
