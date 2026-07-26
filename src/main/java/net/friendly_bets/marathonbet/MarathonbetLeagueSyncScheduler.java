package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Due-check every minute (Europe/Berlin by config): sync each league only in its schedule hours.
 */
@Component
@ConditionalOnProperty(name = "marathonbet.standalone-scheduler", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class MarathonbetLeagueSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarathonbetLeagueSyncScheduler.class);

    private final MarathonbetProperties properties;
    private final MarathonbetSyncService marathonbetSyncService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalDataLayerConfigService layerConfigService;

    /** Keys: {@code yyyy-MM-dd|LEAGUE|hour} already executed. */
    private final Set<String> completedHourRuns = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 * * * * *")
    public void checkDueLeagues() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.ODDS)) {
            return;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(properties.getScheduleZone());
        } catch (Exception e) {
            log.warn("marathonbet invalid schedule-zone={}: {}", properties.getScheduleZone(), e.getMessage());
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate day = now.toLocalDate();
        int hour = now.getHour();
        pruneCompletedKeys(day);

        Optional<Season> active = runningSeasonLookup.findRunningSeason();
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return;
        }

        Set<String> seasonLeagueCodes = new LinkedHashSet<>();
        for (League league : active.get().getLeagues()) {
            if (league != null && league.getLeagueCode() != null) {
                seasonLeagueCodes.add(league.getLeagueCode().name());
            }
        }

        for (String leagueCode : seasonLeagueCodes) {
            if (properties.tournamentTreeIdForLeague(leagueCode) == null) {
                continue;
            }
            if (!properties.isLeagueHourDue(leagueCode, hour)) {
                continue;
            }
            String runKey = day + "|" + leagueCode + "|" + hour;
            if (!completedHourRuns.add(runKey)) {
                continue;
            }
            try {
                log.info("marathonbet due league={} hour={} zone={}", leagueCode, hour, zone);
                marathonbetSyncService.syncLeague(leagueCode);
            } catch (Exception e) {
                completedHourRuns.remove(runKey);
                log.warn("marathonbet league={} sync failed: {}", leagueCode, e.getMessage());
            }
        }
    }

    private void pruneCompletedKeys(LocalDate today) {
        String prefixToday = today + "|";
        String prefixYesterday = today.minusDays(1) + "|";
        completedHourRuns.removeIf(key ->
                !key.startsWith(prefixToday) && !key.startsWith(prefixYesterday));
    }
}
