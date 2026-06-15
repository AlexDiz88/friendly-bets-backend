package net.friendly_bets.fourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.gameresults.GameResultPersistence;
import net.friendly_bets.gameresults.MatchdayPollingTargetResolver;
import net.friendly_bets.gameresults.MatchdaySlotKey;
import net.friendly_bets.fourscore.client.FourScoreHttpClient;
import net.friendly_bets.fourscore.config.FourScoreProperties;
import net.friendly_bets.gameresults.GameResultFinalizer;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchResultStabilizationService;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.gameresults.GameResultsSync;
import net.friendly_bets.models.gameresults.GameResultsSyncStatus;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.GameResultsSyncRepository;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FourScoreSyncService {

    private static final Logger log = LoggerFactory.getLogger(FourScoreSyncService.class);

    private final FourScoreProperties properties;
    private final FourScoreHttpClient httpClient;
    private final FourScoreMatchListParser listParser;
    private final FourScoreEventPageParser eventPageParser;
    private final FourScoreTeamResolver teamResolver;
    private final FourScoreGameResultMapper gameResultMapper;
    private final GameResultPersistence gameResultPersistence;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final MatchdayPollingTargetResolver matchdayPollingTargetResolver;
    private final ApiSyncIssueService apiSyncIssueService;
    private final FourScoreScoreNormalizer scoreNormalizer;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final GameResultsSyncRepository gameResultsSyncRepository;
    private final MatchResultStabilizationService stabilizationService;
    private final GameResultFinalizer gameResultFinalizer;

    public boolean isEnabledForLeague(String leagueCode) {
        return properties.isEnabled()
                && properties.getPrimaryForLeagues() != null
                && properties.getPrimaryForLeagues().contains(leagueCode);
    }

    public int runPollingTick() {
        if (!properties.isEnabled()) {
            return 0;
        }
        Set<FourScoreMatchdayTarget> targets = new LinkedHashSet<>();
        runningSeasonLookup.findRunningSeason().ifPresent(season ->
                targets.addAll(collectMatchdayTargetsForSeason(season)));
        int synced = 0;
        for (FourScoreMatchdayTarget target : targets) {
            try {
                syncMatchday(target.leagueCode(), target.matchday(), target.season(), target.leagueId());
                synced++;
            } catch (Exception e) {
                log.warn("4score sync failed for {}: {}", target, e.getMessage());
            }
        }
        return synced;
    }

    public int syncMatchday(String leagueCode, int matchday, String season, String leagueId) {
        if (!isEnabledForLeague(leagueCode)) {
            return 0;
        }
        List<GameResultRecord> records = new ArrayList<>(gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season));
        fillMissingMatchdayRecords(leagueCode, matchday, season, leagueId, records);
        if (records.isEmpty()) {
            return 0;
        }
        boolean hasPending = records.stream().anyMatch(r -> !r.isFinalized());
        int updated = 0;
        if (hasPending) {
            Set<LocalDate> dates = records.stream()
                    .map(GameResultRecord::getUtcDate)
                    .filter(d -> d != null)
                    .map(LocalDateTime::toLocalDate)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (dates.isEmpty()) {
                LocalDate today = LocalDate.now();
                for (int i = -45; i <= 14; i++) {
                    dates.add(today.plusDays(i));
                }
            }
            for (LocalDate date : dates) {
                updated += syncDateForRecords(date, leagueCode, season, matchday, leagueId, records);
            }
        }
        updateSyncMetadata(leagueCode, matchday, season);
        return updated;
    }

    public GameResultsSync ensureSyncMetadata(String leagueCode, int matchday, String season) {
        return updateSyncMetadata(leagueCode, matchday, season);
    }

    private GameResultsSync updateSyncMetadata(String leagueCode, int matchday, String season) {
        LocalDateTime now = LocalDateTime.now();
        List<GameResultRecord> records = gameResultRecordRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season);
        int finishedCount = (int) records.stream().filter(GameResultRecord::isFinalized).count();
        GameResultsSync sync = gameResultsSyncRepository
                .findByLeagueCodeAndMatchdayAndSeason(leagueCode, matchday, season)
                .orElse(GameResultsSync.builder()
                        .leagueCode(leagueCode)
                        .matchday(matchday)
                        .season(season)
                        .syncStatus(GameResultsSyncStatus.POLLING)
                        .firstFetchedAt(now)
                        .build());
        sync.setExpectedMatchCount(Math.max(sync.getExpectedMatchCount(), records.size()));
        sync.setFinishedMatchCount(finishedCount);
        sync.setLastFetchedAt(now);
        if (sync.getFirstFetchedAt() == null) {
            sync.setFirstFetchedAt(now);
        }
        boolean allFinalized = !records.isEmpty() && finishedCount >= records.size();
        if (allFinalized) {
            sync.setSyncStatus(GameResultsSyncStatus.COMPLETED);
        }
        return gameResultsSyncRepository.save(sync);
    }

    private void fillMissingMatchdayRecords(
            String leagueCode,
            int matchday,
            String season,
            String leagueId,
            List<GameResultRecord> records
    ) {
        LocalDate today = LocalDate.now();
        for (int i = -45; i <= 14; i++) {
            syncDateForRecords(today.plusDays(i), leagueCode, season, matchday, leagueId, records);
        }
    }

    private int syncDateForRecords(
            LocalDate date,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records
    ) {
        String html = httpClient.fetchEventsPage(date);
        List<FourScoreListMatch> listMatches = listParser.parse(html, FourScoreLeagueSection.WORLD_CUP);
        int updated = 0;
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (FourScoreListMatch listMatch : listMatches) {
            try {
                if (applyListMatchIfEligible(
                        listMatch, date, leagueCode, season, matchday, leagueId, records, fetchedAt)) {
                    updated++;
                }
            } catch (Exception e) {
                log.warn("4score match sync failed {}: {}", listMatch.getEventSlug(), e.getMessage());
            }
        }
        return updated;
    }

    private boolean applyListMatchIfEligible(
            FourScoreListMatch listMatch,
            LocalDate listPageDate,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        if (FourScorePlayoffPlaceholderNames.isPlaceholder(listMatch.getHomeTeamName())
                || FourScorePlayoffPlaceholderNames.isPlaceholder(listMatch.getAwayTeamName())) {
            return false;
        }
        Optional<Team> home = teamResolver.resolveOrRecordMissing(
                listMatch.getHomeTeamName(),
                leagueCode,
                season,
                matchday,
                listMatch.getExternalEventId(),
                true
        );
        Optional<Team> away = teamResolver.resolveOrRecordMissing(
                listMatch.getAwayTeamName(),
                leagueCode,
                season,
                matchday,
                listMatch.getExternalEventId(),
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
            Optional<String> berlinSlotId = resolveBerlinSlotId(leagueId, matchday);
            if (berlinSlotId.isPresent()
                    && !WcBerlinSlotMatchFilter.teamPairBelongsToSlot(
                            berlinSlotId.get(), home.get(), away.get())) {
                return false;
            }
            return bootstrapListMatch(
                    listMatch,
                    listPageDate,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    home.get(),
                    away.get(),
                    records,
                    fetchedAt
            );
        }

        if (!needsFourScoreSourceBackfill(existing.get()) && !listMatch.shouldPollForRecord(existing.get())) {
            return false;
        }

        return applyListMatch(listMatch, listPageDate, existing.get(), home.get(), away.get(), fetchedAt);
    }

    private static boolean needsFourScoreSourceBackfill(GameResultRecord record) {
        return record != null && record.fourScoreSource() == null;
    }

    private boolean applyListMatch(
            FourScoreListMatch listMatch,
            LocalDate listPageDate,
            GameResultRecord record,
            Team home,
            Team away,
            LocalDateTime fetchedAt
    ) {
        if (gameResultPersistence.isLockedAgainstApiSync(record)) {
            return false;
        }

        String eventHtml = httpClient.fetchEventPage(listMatch.getEventPath());
        FourScoreEventDetails details = eventPageParser.parse(
                eventHtml,
                listMatch.getEventPath(),
                FourScoreLeagueSection.WORLD_CUP
        );
        GameResultRecord incoming;
        if (details == null) {
            if (!needsFourScoreSourceBackfill(record) || !canBootstrapFromListOnly(listMatch)) {
                return false;
            }
            incoming = gameResultMapper.toIncomingPatchFromList(
                    record,
                    listMatch,
                    home,
                    away,
                    listMatch.getExternalEventId(),
                    listPageDate,
                    fetchedAt
            );
        } else {
            incoming = gameResultMapper.toIncomingPatch(
                    record,
                    details,
                    home,
                    away,
                    listMatch.getExternalEventId(),
                    fetchedAt
            );
        }
        gameResultPersistence.applyProviderSync(
                record,
                incoming,
                MatchDataProviders.FOURSCORE,
                fetchedAt
        );
        if (listMatch.getEventSlug() != null && !listMatch.getEventSlug().isBlank()) {
            record.setFourscoreEventSlug(listMatch.getEventSlug());
        }
        gameResultRecordRepository.save(record);
        return true;
    }

    private boolean bootstrapListMatch(
            FourScoreListMatch listMatch,
            LocalDate listPageDate,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            Team home,
            Team away,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        String eventHtml = httpClient.fetchEventPage(listMatch.getEventPath());
        FourScoreEventDetails details = eventPageParser.parse(
                eventHtml,
                listMatch.getEventPath(),
                FourScoreLeagueSection.WORLD_CUP
        );
        GameResultRecord created;
        if (details == null) {
            if (!canBootstrapFromListOnly(listMatch)) {
                log.warn(
                        "4score bootstrap skipped {}: event page parse failed",
                        listMatch.getEventSlug()
                );
                return false;
            }
            log.warn(
                    "4score bootstrap uses list page only for {} (event page unavailable)",
                    listMatch.getEventSlug()
            );
            created = gameResultMapper.toNewRecordFromList(
                    listMatch,
                    home,
                    away,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    listMatch.getExternalEventId(),
                    listPageDate,
                    fetchedAt
            );
        } else {
            created = gameResultMapper.toNewRecord(
                    details,
                    home,
                    away,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    listMatch.getExternalEventId(),
                    fetchedAt
            );
        }
        if (listMatch.getEventSlug() != null && !listMatch.getEventSlug().isBlank()) {
            created.setFourscoreEventSlug(listMatch.getEventSlug());
        }
        stabilizationService.updateStabilityCounters(created, fetchedAt);
        gameResultFinalizer.tryFinalize(created, fetchedAt);
        GameResultRecord saved = gameResultRecordRepository.save(created);
        records.add(saved);
        return true;
    }

    private static boolean canBootstrapFromListOnly(FourScoreListMatch listMatch) {
        return !listMatch.isTerminal() && !listMatch.needsEventDetails();
    }

    public List<FourScorePreviewMatchDto> previewDate(LocalDate date) {
        String html = httpClient.fetchEventsPage(date);
        List<FourScoreListMatch> listMatches = listParser.parse(
                html,
                EnumSet.of(FourScoreLeagueSection.WORLD_CUP, FourScoreLeagueSection.FRIENDLIES)
        );
        LocalDateTime fetchedAt = LocalDateTime.now();
        return listMatches.stream()
                .map(listMatch -> toPreview(listMatch, fetchedAt))
                .toList();
    }

    private FourScorePreviewMatchDto toPreview(FourScoreListMatch listMatch, LocalDateTime fetchedAt) {
        Optional<Team> home = teamResolver.resolve(listMatch.getHomeTeamName());
        Optional<Team> away = teamResolver.resolve(listMatch.getAwayTeamName());
        FourScoreEventDetails details = null;
        FourScoreScoreNormalizer.NormalizedScore normalized = null;
        String detailsError = null;
        if (listMatch.needsEventDetails()) {
            try {
                String eventHtml = httpClient.fetchEventPageForPreview(listMatch.getEventPath());
                if (eventHtml == null || eventHtml.isBlank()) {
                    detailsError = "emptyEventHtml";
                } else {
                    details = eventPageParser.parse(eventHtml, listMatch.getEventPath(), listMatch.getSection());
                    if (details == null) {
                        detailsError = "eventParseFailed";
                    } else {
                        normalized = scoreNormalizer.normalize(details);
                    }
                }
            } catch (Exception e) {
                detailsError = e.getMessage() != null ? e.getMessage() : "eventFetchFailed";
                log.warn("4score preview event fetch failed {}: {}", listMatch.getEventSlug(), detailsError);
            }
        }
        String fullTimeScore = resolvePreviewFullTimeScore(normalized, details, listMatch);
        String effectiveStatusText = details != null && details.getStatusText() != null
                ? details.getStatusText()
                : listMatch.getStatusText();
        String mappedStatus = normalized != null
                ? normalized.status()
                : FourScoreStatusTextParser.parse(effectiveStatusText).mappedStatus();
        String liveMinuteLabel = normalized != null ? normalized.liveMinuteLabel() : null;
        return FourScorePreviewMatchDto.builder()
                .section(listMatch.getSection().name())
                .eventSlug(listMatch.getEventSlug())
                .eventPath(listMatch.getEventPath())
                .homeTeamName(listMatch.getHomeTeamName())
                .awayTeamName(listMatch.getAwayTeamName())
                .statusText(effectiveStatusText)
                .mappedStatus(mappedStatus)
                .liveMinuteLabel(liveMinuteLabel)
                .listHomeScore(listMatch.getHomeScore())
                .listAwayScore(listMatch.getAwayScore())
                .homeTeamTitle(home.map(Team::getTitle).orElse(null))
                .awayTeamTitle(away.map(Team::getTitle).orElse(null))
                .homeMapped(home.isPresent())
                .awayMapped(away.isPresent())
                .firstHalfScore(details != null ? details.getFirstHalfScore() : null)
                .secondHalfScore(details != null ? details.getSecondHalfScore() : null)
                .extraTimeScore(details != null ? details.getExtraTimeScore() : null)
                .penaltyScore(details != null ? details.getPenaltyScore() : null)
                .fullTimeScore(fullTimeScore)
                .detailsLoaded(details != null)
                .detailsError(detailsError)
                .fetchedAt(fetchedAt)
                .build();
    }

    private static String resolvePreviewFullTimeScore(
            FourScoreScoreNormalizer.NormalizedScore normalized,
            FourScoreEventDetails details,
            FourScoreListMatch listMatch
    ) {
        if (normalized != null && normalized.gameScore() != null && normalized.gameScore().getFullTime() != null) {
            return normalized.gameScore().getFullTime();
        }
        if (details != null
                && details.getHeaderHomeScore() != null
                && details.getHeaderAwayScore() != null) {
            return details.getHeaderHomeScore() + ":" + details.getHeaderAwayScore();
        }
        if (listMatch.getHomeScore() != null && listMatch.getAwayScore() != null) {
            return listMatch.getHomeScore() + ":" + listMatch.getAwayScore();
        }
        return null;
    }

    private Set<FourScoreMatchdayTarget> collectMatchdayTargetsForSeason(Season season) {
        return matchdayPollingTargetResolver.collectForSeason(season, properties.getPrimaryForLeagues()).stream()
                .map(FourScoreSyncService::toFourScoreTarget)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static FourScoreMatchdayTarget toFourScoreTarget(MatchdaySlotKey key) {
        return new FourScoreMatchdayTarget(key.leagueCode(), key.matchday(), key.season(), key.leagueId());
    }

    private Optional<String> resolveBerlinSlotId(String leagueId, int slotOrder) {
        if (leagueId == null || leagueId.isBlank()) {
            return Optional.empty();
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
            return Optional.empty();
        }
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(league.getTournamentFormatId());
        return tournamentFormatExpander.findByOrder(format, slotOrder)
                .map(slot -> slot.getId())
                .filter(WcBerlinSlotMatchFilter::isBerlinGroupSlot);
    }

    private record FourScoreMatchdayTarget(String leagueCode, int matchday, String season, String leagueId) {
    }
}
