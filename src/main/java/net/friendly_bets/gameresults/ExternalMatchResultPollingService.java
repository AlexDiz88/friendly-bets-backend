package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.fourscore.FourScoreSyncService;
import net.friendly_bets.fourscore.config.FourScoreProperties;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.twentyfourscore.TwentyFourScoreSyncService;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Единый poll-цикл: 4score и (опционально) 24score в одном тике, затем стабилизация и финализация.
 */
@Service
@RequiredArgsConstructor
public class ExternalMatchResultPollingService {

    private static final Logger log = LoggerFactory.getLogger(ExternalMatchResultPollingService.class);

    private final FourScoreSyncService fourScoreSyncService;
    private final TwentyFourScoreSyncService twentyFourScoreSyncService;
    private final MatchdayPollingTargetResolver matchdayPollingTargetResolver;
    private final MatchResultSyncSettingsService settingsService;
    private final RunningSeasonLookup runningSeasonLookup;
    private final FourScoreProperties fourScoreProperties;
    private final TwentyFourScoreProperties twentyFourScoreProperties;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultPersistence gameResultPersistence;
    private final AutoBetSettlementService autoBetSettlementService;
    private final GameResultPollingFilter gameResultPollingFilter;

    public void runPollingTick() {
        var settings = settingsService.getEffective();
        Set<MatchdaySlotKey> targets = new LinkedHashSet<>();
        runningSeasonLookup.findRunningSeason().ifPresent(season ->
                targets.addAll(matchdayPollingTargetResolver.collectForSeason(
                        season,
                        fourScoreProperties.getPrimaryForLeagues()
                )));

        for (MatchdaySlotKey key : targets) {
            try {
                pollMatchday(key, settings.isDualVerificationEnabled());
            } catch (Exception e) {
                log.warn("Combined match poll failed for {}: {}", key, e.getMessage());
            }
        }

        try {
            autoBetSettlementService.settleActiveSeasonIfEnabled()
                    .ifPresent(result -> {
                        if (result.isExecuted()) {
                            log.info(
                                    "Auto-settle tick: season={}, matches={}, bets={}",
                                    result.getSeasonId(),
                                    result.getMatchesSubmitted(),
                                    result.getBetsProcessed()
                            );
                        }
                    });
        } catch (Exception e) {
            log.error("Auto-settle tick failed: {}", e.getMessage(), e);
        }
    }

    private void pollMatchday(MatchdaySlotKey key, boolean dualVerification) {
        if (!fourScoreSyncService.isEnabledForLeague(key.leagueCode())) {
            return;
        }
        LocalDateTime fetchedAt = LocalDateTime.now();
        fourScoreSyncService.syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());

        if (dualVerification
                && twentyFourScoreProperties.isEnabled()
                && twentyFourScoreSyncService.isEnabledForLeague(key.leagueCode())) {
            twentyFourScoreSyncService.syncMatchday(
                    key.leagueCode(),
                    key.matchday(),
                    key.season(),
                    key.leagueId()
            );
        }

        List<GameResultRecord> pending = gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(key.leagueCode(), key.matchday(), key.season())
                .stream()
                .filter(record -> !record.isFinalized() && !record.isAdminCorrected())
                .filter(gameResultPollingFilter::needsExternalPoll)
                .toList();

        for (GameResultRecord record : pending) {
            gameResultPersistence.completePollCycle(record, fetchedAt);
            gameResultRecordRepository.save(record);
        }
    }
}
