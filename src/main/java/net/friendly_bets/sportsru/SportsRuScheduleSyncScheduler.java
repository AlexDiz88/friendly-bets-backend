package net.friendly_bets.sportsru;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.providers.LayerProviderRouter;
import net.friendly_bets.providers.ScheduleProvider;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.sportsru.config.SportsRuProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hour-staggered SCHEDULE sync when sports.ru is primary. Only leagues in
 * {@code sportsru.schedule-sync-league-codes} (EPL/BL/CL/LE by default).
 */
@Component
@RequiredArgsConstructor
public class SportsRuScheduleSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SportsRuScheduleSyncScheduler.class);

    private final SportsRuProperties properties;
    private final LayerProviderRouter router;
    private final SportsRuTeamNamesService teamNamesService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalDataLayerConfigService layerConfigService;

    private final Set<String> completedHourRuns = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 * * * * *")
    public void checkDueLeagues() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.SCHEDULE)) {
            return;
        }
        String primary = layerConfigService.assignment(ExternalDataLayer.SCHEDULE).getPrimaryProvider();
        if (!ExternalProviderIds.SPORTS_RU.equals(primary)) {
            return;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(properties.getScheduleZone());
        } catch (Exception e) {
            log.warn("sports.ru invalid schedule-zone={}: {}", properties.getScheduleZone(), e.getMessage());
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate day = now.toLocalDate();
        int hour = now.getHour();
        pruneCompletedKeys(day);

        Optional<Season> seasonOpt = runningSeasonLookup.findRunningSeason();
        if (seasonOpt.isEmpty() || seasonOpt.get().getLeagues() == null) {
            return;
        }
        Season season = seasonOpt.get();
        List<League> leagues = syncableLeagues(season);
        for (int i = 0; i < leagues.size(); i++) {
            League league = leagues.get(i);
            String leagueCode = league.getLeagueCode().name();
            if (!isLeagueHourDue(i, hour)) {
                continue;
            }
            String runKey = day + "|" + leagueCode + "|" + hour;
            if (!completedHourRuns.add(runKey)) {
                continue;
            }
            try {
                applyJitter();
                log.info("sports.ru due league={} hour={} zone={}", leagueCode, hour, zone);
                router.execute(
                        ExternalDataLayer.SCHEDULE,
                        ScheduleProvider.class,
                        p -> p.syncLeague(season, league, true),
                        leagueCode
                );
            } catch (Exception e) {
                completedHourRuns.remove(runKey);
                log.warn("sports.ru schedule sync failed for {}: {}", leagueCode, e.getMessage());
            }
        }
    }

    private List<League> syncableLeagues(Season season) {
        List<League> out = new ArrayList<>();
        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            if (!teamNamesService.isScheduleSyncSupported(league.getLeagueCode())) {
                continue;
            }
            try {
                teamNamesService.requireCalendarPath(league.getLeagueCode());
            } catch (RuntimeException e) {
                continue;
            }
            out.add(league);
        }
        return out;
    }

    boolean isLeagueHourDue(int leagueIndex, int hourOfDay) {
        int step = Math.max(1, properties.getLeagueHourStep());
        int base = Math.floorMod(properties.getLeagueHourBase(), 24);
        int first = Math.floorMod(base + leagueIndex * step, 24);
        int second = Math.floorMod(first + 12, 24);
        return hourOfDay == first || hourOfDay == second;
    }

    private void applyJitter() {
        int minutes = Math.max(0, properties.getSyncJitterMinutes());
        if (minutes <= 0) {
            return;
        }
        long delayMs = ThreadLocalRandom.current().nextLong(0, minutes * 60_000L + 1);
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pruneCompletedKeys(LocalDate today) {
        String prefixToday = today + "|";
        String prefixYesterday = today.minusDays(1) + "|";
        completedHourRuns.removeIf(key ->
                !key.startsWith(prefixToday) && !key.startsWith(prefixYesterday));
    }
}
