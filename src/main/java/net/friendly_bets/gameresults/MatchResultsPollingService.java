package net.friendly_bets.gameresults;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.fourscore.FourScoreSyncService;
import net.friendly_bets.models.Bet;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.repositories.BetsRepository;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.twentyfourscore.TwentyFourScoreSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchResultsPollingService {

    private static final Logger log = LoggerFactory.getLogger(MatchResultsPollingService.class);

    private final BetsRepository betsRepository;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final GetEntityService getEntityService;
    private final MatchdaySlotSupport matchdaySupport;
    private final FourScoreSyncService fourScoreSyncService;
    private final TwentyFourScoreSyncService twentyFourScoreSyncService;

    public void registerPollingForSeason(String seasonId) {
        getEntityService.getSeasonOrThrow(seasonId);
        collectMatchdayKeysForSeasonId(seasonId).forEach(this::ensurePolling);
    }

    public void registerPollingForOpenedBet(Bet bet) {
        if (bet.getBetStatus() != Bet.BetStatus.OPENED || bet.getMatchDay() == null || bet.getMatchDay().isBlank()) {
            return;
        }
        String leagueId = extractLeagueId(bet);
        String seasonId = extractSeasonId(bet);
        if (leagueId == null || seasonId == null) {
            return;
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        matchdaySupport.buildMatchdayKey(
                        league,
                        bet.getMatchDay(),
                        matchdaySupport.resolveExternalSeasonYear(season, league.getLeagueCode()))
                .ifPresent(this::ensurePolling);
    }

    public void refreshSeason(String seasonId) {
        Set<MatchdaySlotKey> keys = collectMatchdayKeysForSeasonId(seasonId);
        for (MatchdaySlotKey key : keys) {
            ensurePolling(key);
            syncMatchdayIfEnabled(key);
        }
    }

    private void syncMatchdayIfEnabled(MatchdaySlotKey key) {
        if (!fourScoreSyncService.isEnabledForLeague(key.leagueCode())) {
            return;
        }
        try {
            fourScoreSyncService.syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
            if (twentyFourScoreSyncService.isEnabledForLeague(key.leagueCode())) {
                twentyFourScoreSyncService.syncMatchday(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
            }
        } catch (Exception e) {
            log.warn("Season refresh sync failed for {}: {}", key, e.getMessage());
        }
    }

    private Set<MatchdaySlotKey> collectMatchdayKeysForSeasonId(String seasonId) {
        Season season = getEntityService.getSeasonOrThrow(seasonId);
        return collectMatchdayKeysForSeason(season);
    }

    private Set<MatchdaySlotKey> collectMatchdayKeysForSeason(Season season) {
        List<Bet> openedBets = betsRepository.findAllBySeason_IdAndBetStatus(season.getId(), Bet.BetStatus.OPENED);
        Set<MatchdaySlotKey> keys = new LinkedHashSet<>();
        int skipped = 0;

        for (Bet bet : openedBets) {
            if (bet.getMatchDay() == null || bet.getMatchDay().isBlank()) {
                skipped++;
                continue;
            }
            String leagueId = extractLeagueId(bet);
            if (leagueId == null) {
                skipped++;
                continue;
            }
            try {
                League league = getEntityService.getLeagueOrThrow(leagueId);
                String externalSeason = matchdaySupport.resolveExternalSeasonYear(season, league.getLeagueCode());
                Optional<MatchdaySlotKey> matchdayKey = matchdaySupport.buildMatchdayKey(
                        league, bet.getMatchDay(), externalSeason);
                if (matchdayKey.isPresent()) {
                    keys.add(matchdayKey.get());
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.warn(
                        "Match-results polling: skip bet {} (league {}): {}",
                        bet.getId(),
                        leagueId,
                        e.getMessage()
                );
                skipped++;
            }
        }

        if (!openedBets.isEmpty()) {
            log.info(
                    "Match-results polling keys from {} OPENED bet(s): {} matchday(s), {} skipped",
                    openedBets.size(),
                    keys.size(),
                    skipped
            );
        }
        return keys;
    }

    private void ensurePolling(MatchdaySlotKey key) {
        gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(key.leagueCode(), key.matchday(), key.season())
                .ifPresentOrElse(existing -> {
                    if (existing.getSyncStatus() == GameResultsSyncStatus.COMPLETED) {
                        existing.setSyncStatus(GameResultsSyncStatus.POLLING);
                        existing.setCompletedAt(null);
                        gameResultsSyncRepository.save(existing);
                        log.debug("Reopened polling for matchday (new opened bets): {}", key);
                    }
                }, () -> gameResultsSyncRepository.save(GameResultsSync.builder()
                        .leagueCode(key.leagueCode())
                        .matchday(key.matchday())
                        .season(key.season())
                        .syncStatus(GameResultsSyncStatus.POLLING)
                        .expectedMatchCount(0)
                        .finishedMatchCount(0)
                        .firstFetchedAt(GameResultNotStarted.nowUtc())
                        .lastFetchedAt(null)
                        .build()));
    }

    private static String extractLeagueId(Bet bet) {
        if (bet.getLeague() == null) {
            return null;
        }
        String leagueId = bet.getLeague().getId();
        return leagueId == null || leagueId.isBlank() ? null : leagueId;
    }

    private static String extractSeasonId(Bet bet) {
        if (bet.getSeason() == null) {
            return null;
        }
        String seasonId = bet.getSeason().getId();
        return seasonId == null || seasonId.isBlank() ? null : seasonId;
    }
}
