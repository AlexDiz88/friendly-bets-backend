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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Staggered schedule sync for all leagues of the running season that have a competition id.
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
                log.warn("soccer365 schedule sync failed for {}: {}", leagueCode, e.getMessage());
            } finally {
                lastSyncAt.put(leagueCode, Instant.now());
            }
        }
    }

    private List<League> syncableLeagues(Season season) {
        List<League> out = new ArrayList<>();
        Map<String, Integer> competitionIds = properties.getCompetitionIds();
        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            Integer competitionId = competitionIds != null
                    ? competitionIds.get(league.getLeagueCode().name())
                    : null;
            if (competitionId == null || competitionId <= 0) {
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
