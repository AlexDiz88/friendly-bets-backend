package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.GameResultPersistence;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchdayPollingTargetResolver;
import net.friendly_bets.gameresults.MatchdaySlotKey;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.twentyfourscore.client.TwentyFourScoreHttpClient;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TwentyFourScoreSyncService {

    private static final Logger log = LoggerFactory.getLogger(TwentyFourScoreSyncService.class);

    private final TwentyFourScoreProperties properties;
    private final TwentyFourScoreHttpClient httpClient;
    private final TwentyFourScoreScheduleParser scheduleParser;
    private final TwentyFourScoreMatchPageParser matchPageParser;
    private final TwentyFourScoreTeamResolver teamResolver;
    private final TwentyFourScoreGameResultMapper gameResultMapper;
    private final GameResultPersistence gameResultPersistence;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdayPollingTargetResolver matchdayPollingTargetResolver;

    public boolean isEnabledForLeague(String leagueCode) {
        return properties.isEnabled()
                && properties.getSecondaryForLeagues() != null
                && properties.getSecondaryForLeagues().contains(leagueCode);
    }

    public int runPollingTick() {
        if (!properties.isEnabled()) {
            return 0;
        }
        Set<TwentyFourScoreMatchdayTarget> targets = new LinkedHashSet<>();
        runningSeasonLookup.findRunningSeason().ifPresent(season ->
                targets.addAll(collectMatchdayTargetsForSeason(season)));
        int synced = 0;
        for (TwentyFourScoreMatchdayTarget target : targets) {
            try {
                synced += syncMatchday(target.leagueCode(), target.matchday(), target.season(), target.leagueId());
            } catch (Exception e) {
                log.warn("24score sync failed for {}: {}", target, e.getMessage());
            }
        }
        return synced;
    }

    public int syncMatchday(String leagueCode, int matchday, String season, String leagueId) {
        if (!isEnabledForLeague(leagueCode)) {
            return 0;
        }
        List<GameResultRecord> records = gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season);
        if (records.isEmpty()) {
            return 0;
        }
        boolean hasPending = records.stream().anyMatch(r -> !r.isFinalized());
        if (!hasPending) {
            return 0;
        }
        Set<LocalDate> dates = records.stream()
                .map(GameResultRecord::getUtcDate)
                .filter(d -> d != null)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dates.isEmpty()) {
            dates.add(LocalDate.now());
        }
        int updated = 0;
        for (LocalDate date : dates) {
            updated += syncDateForRecords(date, leagueCode, season, matchday, records);
        }
        return updated;
    }

    private int syncDateForRecords(
            LocalDate date,
            String leagueCode,
            String season,
            int matchday,
            List<GameResultRecord> records
    ) {
        String html = httpClient.fetchDailyPage(date);
        List<TwentyFourScoreListMatch> listMatches = scheduleParser.parseDailyPage(
                html,
                date,
                TwentyFourScoreCompetitionMapping.worldCupPathMarker()
        );
        int updated = 0;
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (TwentyFourScoreListMatch listMatch : listMatches) {
            try {
                if (applyListMatchIfEligible(listMatch, leagueCode, season, matchday, records, fetchedAt)) {
                    updated++;
                }
            } catch (Exception e) {
                log.warn("24score match sync failed {}: {}", listMatch.getMatchPath(), e.getMessage());
            }
        }
        return updated;
    }

    private boolean applyListMatchIfEligible(
            TwentyFourScoreListMatch listMatch,
            String leagueCode,
            String season,
            int matchday,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        Optional<Team> home = teamResolver.resolveOrRecordMissing(
                listMatch.getHomeTeamName(),
                leagueCode,
                season,
                matchday,
                listMatch.getExternalMatchId(),
                true
        );
        Optional<Team> away = teamResolver.resolveOrRecordMissing(
                listMatch.getAwayTeamName(),
                leagueCode,
                season,
                matchday,
                listMatch.getExternalMatchId(),
                false
        );
        if (home.isEmpty() || away.isEmpty()) {
            return false;
        }
        Optional<GameResultRecord> existing = records.stream()
                .filter(r -> home.get().getId().equals(r.getHomeTeamId())
                        && away.get().getId().equals(r.getAwayTeamId()))
                .findFirst();
        if (existing.isEmpty()) {
            return false;
        }
        return applyListMatch(listMatch, existing.get(), home.get(), away.get(), fetchedAt);
    }

    private boolean applyListMatch(
            TwentyFourScoreListMatch listMatch,
            GameResultRecord record,
            Team home,
            Team away,
            LocalDateTime fetchedAt
    ) {
        if (gameResultPersistence.isLockedAgainstApiSync(record)) {
            return false;
        }
        GameResultRecord incoming;
        if (needsMatchPage(listMatch)) {
            String matchHtml = httpClient.fetchMatchPage(listMatch.getMatchPath());
            TwentyFourScoreMatchDetails details = matchPageParser.parse(matchHtml, listMatch.getMatchPath());
            if (details == null) {
                incoming = gameResultMapper.toSecondaryPatchFromList(record, listMatch, home, away, fetchedAt);
            } else {
                incoming = gameResultMapper.toSecondaryPatch(record, details, home, away, fetchedAt);
            }
        } else {
            incoming = gameResultMapper.toSecondaryPatchFromList(record, listMatch, home, away, fetchedAt);
        }
        gameResultPersistence.applyProviderSync(
                record,
                incoming,
                MatchDataProviders.TWENTYFOUR_SCORE,
                fetchedAt
        );
        gameResultRecordRepository.save(record);
        return true;
    }

    private static boolean needsMatchPage(TwentyFourScoreListMatch listMatch) {
        return listMatch.getFullTimeScore() == null || listMatch.getFirstHalfScore() == null;
    }

    private Set<TwentyFourScoreMatchdayTarget> collectMatchdayTargetsForSeason(Season season) {
        return matchdayPollingTargetResolver.collectForSeason(season, properties.getSecondaryForLeagues()).stream()
                .map(TwentyFourScoreSyncService::toTwentyFourScoreTarget)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static TwentyFourScoreMatchdayTarget toTwentyFourScoreTarget(MatchdaySlotKey key) {
        return new TwentyFourScoreMatchdayTarget(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
    }

    private record TwentyFourScoreMatchdayTarget(String leagueCode, int matchday, String season, String leagueId) {
    }
}
