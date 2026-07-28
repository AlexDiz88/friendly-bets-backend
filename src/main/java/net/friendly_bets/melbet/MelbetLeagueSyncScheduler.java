package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.services.ExternalDataLayerConfigService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Due-check every minute: sync each league in its schedule hours when Melbet is ODDS primary.
 */
@Component
@RequiredArgsConstructor
public class MelbetLeagueSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MelbetLeagueSyncScheduler.class);

    private final MelbetProperties properties;
    private final MelbetSyncService melbetSyncService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final ExternalDataLayerConfigService layerConfigService;

    private final Set<String> completedHourRuns = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 * * * * *")
    public void checkDueLeagues() {
        if (!layerConfigService.isLayerEnabled(ExternalDataLayer.ODDS)) {
            return;
        }
        String primary = layerConfigService.assignment(ExternalDataLayer.ODDS).getPrimaryProvider();
        if (!ExternalProviderIds.MELBET.equals(primary)) {
            return;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(properties.getScheduleZone());
        } catch (Exception e) {
            log.warn("melbet invalid schedule-zone={}: {}", properties.getScheduleZone(), e.getMessage());
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
            if (properties.tournamentIdForLeague(leagueCode) == null) {
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
                melbetSyncService.syncLeague(leagueCode, true);
            } catch (Exception e) {
                log.warn("melbet scheduled sync failed league={}: {}", leagueCode, e.getMessage());
                completedHourRuns.remove(runKey);
            }
        }
    }

    private void pruneCompletedKeys(LocalDate today) {
        String prefix = today.toString() + "|";
        completedHourRuns.removeIf(key -> !key.startsWith(prefix));
    }
}
