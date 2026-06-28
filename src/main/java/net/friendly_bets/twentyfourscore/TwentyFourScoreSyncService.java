package net.friendly_bets.twentyfourscore;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.fourscore.FourScoreKickoffUtc;
import net.friendly_bets.fourscore.FourScoreListDates;
import net.friendly_bets.fourscore.FourScorePlayoffPlaceholderNames;
import net.friendly_bets.gameresults.GameResultPersistence;
import net.friendly_bets.gameresults.GameResultQueryService;
import net.friendly_bets.gameresults.MatchDataProviders;
import net.friendly_bets.gameresults.MatchdayPollingTargetResolver;
import net.friendly_bets.gameresults.MatchdaySlotKey;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.services.TeamAliasResolver;
import net.friendly_bets.services.TournamentFormatExpander;
import net.friendly_bets.twentyfourscore.client.TwentyFourScoreHttpClient;
import net.friendly_bets.twentyfourscore.config.TwentyFourScoreProperties;
import net.friendly_bets.wc26.WcBerlinSlotMatchFilter;
import net.friendly_bets.wc26.Wc26ScheduleCatalog;
import net.friendly_bets.wc26.Wc26ScheduleKickoffResolver;
import net.friendly_bets.wc26.Wc26ScheduleLinker;
import net.friendly_bets.wc26.Wc26TeamCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final TwentyFourScoreScoreNormalizer scoreNormalizer;
    private final TwentyFourScoreGameResultMapper gameResultMapper;
    private final GameResultPersistence gameResultPersistence;
    private final GameResultQueryService gameResultQueryService;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final RunningSeasonLookup runningSeasonLookup;
    private final MatchdayPollingTargetResolver matchdayPollingTargetResolver;
    private final GetEntityService getEntityService;
    private final TournamentFormatExpander tournamentFormatExpander;
    private final Wc26ScheduleLinker wc26ScheduleLinker;
    private final Wc26ScheduleKickoffResolver wc26ScheduleKickoffResolver;
    private final TeamAliasResolver teamAliasResolver;
    private final TeamsRepository teamsRepository;

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
        List<GameResultRecord> records = new ArrayList<>(
                gameResultQueryService.listMatchdayRecordsForSync(leagueCode, matchday, season));
        if ("WC".equals(leagueCode)) {
            wc26ScheduleLinker.relinkMatchdayRecords(records);
        }
        Optional<String> wcSlotId = resolveWcBettingSlotId(leagueId, matchday, records);
        int wcExpected = wcSlotId.map(WcBerlinSlotMatchFilter::expectedMatchCount).orElse(0);
        boolean slotIncomplete = wcExpected > 0 && records.size() < wcExpected;
        if (("WC".equals(leagueCode) && records.isEmpty()) || slotIncomplete) {
            bootstrapInitialWcSlotRecords(leagueCode, season, matchday, leagueId, records, wcSlotId);
        }
        wcExpected = wcSlotId.map(WcBerlinSlotMatchFilter::expectedMatchCount).orElse(0);
        slotIncomplete = wcExpected > 0 && records.size() < wcExpected;
        List<GameResultRecord> pending = records.stream()
                .filter(r -> !r.isFinalized())
                .toList();
        int updated = 0;
        if (!pending.isEmpty() || slotIncomplete) {
            Set<LocalDate> dates = resolveListFetchDates(leagueCode, leagueId, matchday, records);
            for (LocalDate date : dates) {
                updated += syncDateForRecords(date, leagueCode, season, matchday, leagueId, records);
            }
            if (slotIncomplete && missingCatalogPairCount(wcSlotId.get(), records) > 0) {
                updated += fillWcSlotGapsFromSecondary(leagueCode, season, matchday, leagueId, records, wcSlotId);
            }
        } else if ("WC".equals(leagueCode) && wcSlotId.isPresent()
                && missingCatalogPairCount(wcSlotId.get(), records) > 0) {
            updated += bootstrapMissingCatalogPairs(
                    wcSlotId.get(), leagueCode, season, matchday, leagueId, records, LocalDateTime.now());
        }
        return updated;
    }

    /**
     * 4score: placeholder на одной стороне (напр. «2nd Group J») — bootstrap той же пары из 24score list-страницы.
     */
    public void tryBootstrapAfterFourScorePlaceholder(
            String homeTeamName,
            String awayTeamName,
            LocalDate listPageDate,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records
    ) {
        // Playoff placeholders are not bootstrapped — only resolved teams after manual sync.
    }

    /**
     * После primary (4score): bootstrap матчей слота, которые не попали из-за placeholder / unmapped sides.
     */
    public int fillWcSlotGapsFromSecondary(
            String leagueCode,
            int matchday,
            String season,
            String leagueId
    ) {
        if (!isEnabledForLeague(leagueCode) || !"WC".equals(leagueCode)) {
            return 0;
        }
        List<GameResultRecord> records = new ArrayList<>(
                gameResultQueryService.listMatchdayRecordsForSync(leagueCode, matchday, season));
        if ("WC".equals(leagueCode)) {
            wc26ScheduleLinker.relinkMatchdayRecords(records);
        }
        Optional<String> wcSlotId = resolveWcBettingSlotId(leagueId, matchday, records);
        return fillWcSlotGapsFromSecondary(leagueCode, season, matchday, leagueId, records, wcSlotId);
    }

    private int fillWcSlotGapsFromSecondary(
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            Optional<String> wcSlotId
    ) {
        if (wcSlotId.isEmpty()) {
            return 0;
        }
        if (missingCatalogPairCount(wcSlotId.get(), records) == 0) {
            return 0;
        }
        Set<LocalDate> dates = new LinkedHashSet<>(
                wc26ScheduleKickoffResolver.listPageDatesForWcSlot(wcSlotId.get()));
        dates.add(FourScoreListDates.todayInListZone());
        int updated = 0;
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (LocalDate date : dates) {
            updated += syncDateForRecords(date, leagueCode, season, matchday, leagueId, records);
        }
        Optional<String> slotId = wcSlotId.isPresent()
                ? wcSlotId
                : resolveWcBettingSlotId(leagueId, matchday, records);
        if (slotId.isPresent()) {
            updated += bootstrapMissingCatalogPairs(
                    slotId.get(),
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    records,
                    fetchedAt
            );
        }
        if (records.size() < WcBerlinSlotMatchFilter.expectedMatchCount(wcSlotId.get())) {
            log.info(
                    "24score WC slot gap fill: {}/{} matches for {} (matchday {})",
                    records.size(),
                    WcBerlinSlotMatchFilter.expectedMatchCount(wcSlotId.get()),
                    wcSlotId.get(),
                    matchday
            );
        }
        return updated;
    }

    public List<TwentyFourScorePreviewMatchDto> previewDate(LocalDate date) {
        String html = httpClient.fetchDailyPage(date);
        List<TwentyFourScoreListMatch> listMatches = scheduleParser.parseDailyPagePreview(html, date);
        LocalDateTime fetchedAt = LocalDateTime.now();
        return listMatches.stream()
                .map(listMatch -> toPreview(listMatch, fetchedAt))
                .toList();
    }

    private TwentyFourScorePreviewMatchDto toPreview(TwentyFourScoreListMatch listMatch, LocalDateTime fetchedAt) {
        Optional<Team> home = teamResolver.resolve(listMatch.getHomeTeamName());
        Optional<Team> away = teamResolver.resolve(listMatch.getAwayTeamName());
        TwentyFourScoreMatchDetails details = null;
        TwentyFourScoreScoreNormalizer.NormalizedScore normalized = scoreNormalizer.normalize(listMatch);
        String detailsError = null;
        if (needsMatchPage(listMatch)) {
            try {
                String matchHtml = httpClient.fetchMatchPage(listMatch.getMatchPath());
                if (matchHtml == null || matchHtml.isBlank()) {
                    detailsError = "emptyMatchHtml";
                } else {
                    details = matchPageParser.parse(matchHtml, listMatch.getMatchPath());
                    if (details == null) {
                        detailsError = "matchParseFailed";
                    } else {
                        normalized = scoreNormalizer.normalize(details);
                    }
                }
            } catch (Exception e) {
                detailsError = e.getMessage() != null ? e.getMessage() : "matchFetchFailed";
                log.warn("24score preview match fetch failed {}: {}", listMatch.getMatchPath(), detailsError);
            }
        }
        String effectiveStatusText = details != null && details.getStatusText() != null
                ? details.getStatusText()
                : listMatch.getStatusText();
        String mappedStatus = normalized != null
                ? normalized.status()
                : TwentyFourScoreStatusMapper.mapStatus(effectiveStatusText);
        String liveMinuteLabel = normalized != null ? normalized.liveMinuteLabel() : null;
        String fullTimeScore = resolvePreviewFullTimeScore(normalized, details, listMatch);
        return TwentyFourScorePreviewMatchDto.builder()
                .section(listMatch.getSection() != null ? listMatch.getSection() : TwentyFourScoreLeagueSection.WORLD_CUP.name())
                .eventSlug(String.valueOf(listMatch.getExternalMatchId()))
                .eventPath(listMatch.getMatchPath())
                .homeTeamName(listMatch.getHomeTeamName())
                .awayTeamName(listMatch.getAwayTeamName())
                .statusText(effectiveStatusText)
                .mappedStatus(mappedStatus)
                .liveMinuteLabel(liveMinuteLabel)
                .listHomeScore(parseListScore(fullTimeScore, true))
                .listAwayScore(parseListScore(fullTimeScore, false))
                .homeTeamTitle(home.map(Team::getTitle).orElse(null))
                .awayTeamTitle(away.map(Team::getTitle).orElse(null))
                .homeMapped(home.isPresent())
                .awayMapped(away.isPresent())
                .firstHalfScore(details != null ? details.getFirstHalfScore() : listMatch.getFirstHalfScore())
                .secondHalfScore(null)
                .extraTimeScore(details != null ? details.getExtraTimeScore() : listMatch.getExtraTimeScore())
                .penaltyScore(details != null ? details.getPenaltyScore() : listMatch.getPenaltyScore())
                .fullTimeScore(fullTimeScore)
                .detailsLoaded(details != null)
                .detailsError(detailsError)
                .fetchedAt(fetchedAt)
                .build();
    }

    private static String resolvePreviewFullTimeScore(
            TwentyFourScoreScoreNormalizer.NormalizedScore normalized,
            TwentyFourScoreMatchDetails details,
            TwentyFourScoreListMatch listMatch
    ) {
        if (normalized != null
                && normalized.gameScore() != null
                && normalized.gameScore().getFullTime() != null) {
            return normalized.gameScore().getFullTime();
        }
        if (details != null && details.getFullTimeScore() != null) {
            return details.getFullTimeScore();
        }
        return listMatch.getFullTimeScore();
    }

    private static Integer parseListScore(String fullTimeScore, boolean home) {
        if (fullTimeScore == null || fullTimeScore.isBlank() || !fullTimeScore.contains(":")) {
            return null;
        }
        String[] parts = fullTimeScore.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Integer.parseInt(parts[home ? 0 : 1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void bootstrapInitialWcSlotRecords(
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            Optional<String> slotId
    ) {
        if (slotId.isEmpty() || WcTournamentSlots.scheduleIdsForSlot(slotId.get()).isEmpty()) {
            return;
        }
        Set<LocalDate> dates = new LinkedHashSet<>(
                wc26ScheduleKickoffResolver.listPageDatesForWcSlot(slotId.get()));
        dates.add(FourScoreListDates.todayInListZone());
        LocalDateTime fetchedAt = LocalDateTime.now();
        for (LocalDate date : dates) {
            syncDateForRecords(date, leagueCode, season, matchday, leagueId, records);
        }
    }

    private Set<LocalDate> resolveListFetchDates(
            String leagueCode,
            String leagueId,
            int matchday,
            List<GameResultRecord> records
    ) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (GameResultRecord record : records) {
            if (record.getUtcDate() != null) {
                dates.add(FourScoreListDates.listPageDateFromStoredUtc(record.getUtcDate()));
            }
        }
        if ("WC".equals(leagueCode)) {
            resolveWcBettingSlotId(leagueId, matchday, records)
                    .ifPresent(slotId -> dates.addAll(wc26ScheduleKickoffResolver.listPageDatesForWcSlot(slotId)));
        }
        if (dates.isEmpty()) {
            dates.add(FourScoreListDates.todayInListZone());
        }
        return dates;
    }

    private int syncDateForRecords(
            LocalDate date,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
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
                if (applyListMatchIfEligible(
                        listMatch, leagueCode, season, matchday, leagueId, records, fetchedAt)) {
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
            if (!"WC".equals(leagueCode)) {
                return false;
            }
            Optional<String> wcSlotId = resolveWcBettingSlotId(leagueId, matchday, records);
            if (wcSlotId.isEmpty()) {
                return false;
            }
            LocalDateTime kickoff = listKickoffUtc(listMatch);
            if (kickoff == null) {
                kickoff = gameResultMapper.resolveWcKickoffUtc(
                        leagueCode, listMatch, home.get(), away.get());
            }
            Optional<Integer> catalogScheduleId = Wc26ScheduleCatalog.findByTeamPair(
                            teamFifa(home.get()), teamFifa(away.get()))
                    .map(Wc26ScheduleCatalog.GroupMatch::scheduleId);
            Optional<Integer> scheduleId = catalogScheduleId;
            if (scheduleId.isEmpty() && kickoff != null) {
                scheduleId = WcBerlinSlotMatchFilter.resolveScheduleIdInSlot(wcSlotId.get(), kickoff);
            }
            if (!WcBerlinSlotMatchFilter.matchBelongsToWcSlot(
                    wcSlotId.get(), home.get(), away.get(), kickoff, scheduleId.orElse(null))) {
                return false;
            }
            return bootstrapListMatch(
                    listMatch,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    home.get(),
                    away.get(),
                    records,
                    fetchedAt,
                    scheduleId.orElse(null)
            );
        }
        return applyListMatch(listMatch, existing.get(), home.get(), away.get(), fetchedAt);
    }

    private boolean bootstrapListMatch(
            TwentyFourScoreListMatch listMatch,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            Team home,
            Team away,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt,
            Integer scheduleId
    ) {
        GameResultRecord created = gameResultMapper.toNewRecord(
                listMatch,
                home,
                away,
                leagueCode,
                season,
                matchday,
                leagueId,
                fetchedAt
        );
        GameResultRecord saved = gameResultRecordRepository.save(created);
        if (scheduleId != null) {
            saved.setWc26ScheduleId(scheduleId);
        }
        wc26ScheduleLinker.linkIfNeeded(saved, home, away);
        wc26ScheduleLinker.backfillKickoffFromSchedule(saved);
        saved = gameResultRecordRepository.save(saved);
        records.add(saved);
        log.info(
                "24score WC slot bootstrap: {} vs {} (matchday {}, schedule {})",
                listMatch.getHomeTeamName(),
                listMatch.getAwayTeamName(),
                matchday,
                saved.getWc26ScheduleId()
        );
        return true;
    }

    private boolean bootstrapListMatch(
            TwentyFourScoreListMatch listMatch,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            Team home,
            Team away,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        return bootstrapListMatch(
                listMatch, leagueCode, season, matchday, leagueId, home, away, records, fetchedAt, null);
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
        wc26ScheduleLinker.linkIfNeeded(record, home, away);
        wc26ScheduleLinker.backfillKickoffFromSchedule(record);
        gameResultRecordRepository.save(record);
        return true;
    }

    private static boolean needsMatchPage(TwentyFourScoreListMatch listMatch) {
        return listMatch.getFullTimeScore() == null || listMatch.getFirstHalfScore() == null;
    }

    /**
     * Для каждого schedule_id слота без записи в game_results — ищем матч на list-странице 24score по kickoff.
     */
    private int bootstrapMissingCatalogPairs(
            String slotId,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        int bootstrapped = 0;
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            if (scheduleSlotFilled(scheduleId, records)) {
                continue;
            }
            if (tryBootstrapScheduleIdFromListPage(
                    scheduleId,
                    slotId,
                    null,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    records,
                    fetchedAt
            )) {
                bootstrapped++;
                log.info("24score WC gap bootstrap: schedule {} (slot {})", scheduleId, slotId);
            } else {
                log.info("24score WC gap: no list match for schedule {} (slot {})", scheduleId, slotId);
            }
        }
        return bootstrapped;
    }

    private int missingCatalogPairCount(String slotId, List<GameResultRecord> records) {
        int missing = 0;
        for (int scheduleId : WcTournamentSlots.scheduleIdsForSlot(slotId)) {
            if (!scheduleSlotFilled(scheduleId, records)) {
                missing++;
            }
        }
        return missing;
    }

    private boolean scheduleSlotFilled(int scheduleId, List<GameResultRecord> records) {
        return records.stream().anyMatch(r -> Integer.valueOf(scheduleId).equals(r.getWc26ScheduleId()));
    }

    private boolean recordMatchesCatalogPair(
            Team home,
            Team away,
            Wc26ScheduleCatalog.GroupMatch catalog
    ) {
        String homeFifa = teamFifa(home);
        String awayFifa = teamFifa(away);
        return homeFifa != null
                && awayFifa != null
                && catalog.homeFifa().equalsIgnoreCase(homeFifa)
                && catalog.awayFifa().equalsIgnoreCase(awayFifa);
    }

    private boolean recordMatchesCatalogPair(
            GameResultRecord record,
            Wc26ScheduleCatalog.GroupMatch catalog
    ) {
        if (record.getHomeTeamId() == null || record.getAwayTeamId() == null) {
            return false;
        }
        Optional<Team> home = teamsRepository.findById(record.getHomeTeamId());
        Optional<Team> away = teamsRepository.findById(record.getAwayTeamId());
        if (home.isEmpty() || away.isEmpty()) {
            return false;
        }
        String homeFifa = teamFifa(home.get());
        String awayFifa = teamFifa(away.get());
        return catalog.homeFifa().equalsIgnoreCase(homeFifa)
                && catalog.awayFifa().equalsIgnoreCase(awayFifa);
    }

    /**
     * Ищем матч на list-странице 24score по kickoff FIFA-расписания.
     */
    private boolean tryBootstrapScheduleIdFromListPage(
            int scheduleId,
            String slotId,
            LocalDate preferredListDate,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        if (preferredListDate != null) {
            dates.add(preferredListDate);
        }
        wc26ScheduleKickoffResolver.kickoffUtc(scheduleId)
                .map(FourScoreListDates::listPageDateFromKickoffUtc)
                .ifPresent(dates::add);
        dates.add(FourScoreListDates.todayInListZone());
        for (LocalDate listDate : dates) {
            if (tryBootstrapScheduleIdOnDate(
                    scheduleId,
                    slotId,
                    listDate,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    records,
                    fetchedAt
            )) {
                return true;
            }
        }
        return false;
    }

    private boolean tryBootstrapScheduleIdOnDate(
            int scheduleId,
            String slotId,
            LocalDate listDate,
            String leagueCode,
            String season,
            int matchday,
            String leagueId,
            List<GameResultRecord> records,
            LocalDateTime fetchedAt
    ) {
        String html = httpClient.fetchDailyPage(listDate);
        List<TwentyFourScoreListMatch> listMatches = scheduleParser.parseDailyPage(
                html,
                listDate,
                TwentyFourScoreCompetitionMapping.worldCupPathMarker()
        );
        for (TwentyFourScoreListMatch listMatch : listMatches) {
            if (FourScorePlayoffPlaceholderNames.isPlaceholder(listMatch.getHomeTeamName())
                    || FourScorePlayoffPlaceholderNames.isPlaceholder(listMatch.getAwayTeamName())) {
                continue;
            }
            Optional<Team> home = teamResolver.resolve(listMatch.getHomeTeamName());
            Optional<Team> away = teamResolver.resolve(listMatch.getAwayTeamName());
            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }
            boolean matchesSchedule = Wc26ScheduleCatalog.findById(scheduleId)
                    .filter(c -> c.homeFifa() != null && c.awayFifa() != null)
                    .map(c -> recordMatchesCatalogPair(home.get(), away.get(), c))
                    .orElse(false);
            if (!matchesSchedule) {
                LocalDateTime kickoff = listKickoffUtc(listMatch);
                if (kickoff == null) {
                    continue;
                }
                Optional<Integer> resolvedScheduleId = WcBerlinSlotMatchFilter.resolveScheduleIdInSlot(
                        slotId, kickoff);
                if (resolvedScheduleId.isEmpty() || resolvedScheduleId.get() != scheduleId) {
                    continue;
                }
            }
            if (records.stream().anyMatch(r ->
                    home.get().getId().equals(r.getHomeTeamId())
                            && away.get().getId().equals(r.getAwayTeamId()))) {
                GameResultRecord existing = records.stream()
                        .filter(r -> home.get().getId().equals(r.getHomeTeamId())
                                && away.get().getId().equals(r.getAwayTeamId()))
                        .findFirst()
                        .orElseThrow();
                if (!Integer.valueOf(scheduleId).equals(existing.getWc26ScheduleId())) {
                    existing.setWc26ScheduleId(scheduleId);
                    wc26ScheduleLinker.backfillKickoffFromSchedule(existing);
                    gameResultRecordRepository.save(existing);
                }
                return true;
            }
            boolean bootstrapped = bootstrapListMatch(
                    listMatch,
                    leagueCode,
                    season,
                    matchday,
                    leagueId,
                    home.get(),
                    away.get(),
                    records,
                    fetchedAt
            );
            if (bootstrapped) {
                GameResultRecord saved = records.get(records.size() - 1);
                saved.setWc26ScheduleId(scheduleId);
                gameResultRecordRepository.save(saved);
            }
            return bootstrapped;
        }
        return false;
    }

    private static LocalDateTime listKickoffUtc(TwentyFourScoreListMatch listMatch) {
        if (listMatch.getMatchDate() == null || listMatch.getKickoffTime() == null) {
            return null;
        }
        return FourScoreKickoffUtc.fromMoscowLocal(listMatch.getMatchDate(), listMatch.getKickoffTime());
    }

    private Optional<String> resolveWcBettingSlotId(
            String leagueId,
            int slotOrder,
            List<GameResultRecord> records
    ) {
        Optional<String> fromLeague = resolveWcBettingSlotId(leagueId, slotOrder);
        if (fromLeague.isPresent()) {
            return fromLeague;
        }
        if (records == null) {
            return Optional.empty();
        }
        return records.stream()
                .map(GameResultRecord::getLeagueId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .flatMap(id -> resolveWcBettingSlotId(id, slotOrder));
    }

    private Optional<String> resolveWcBettingSlotId(String leagueId, int slotOrder) {
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
                .filter(WcBerlinSlotMatchFilter::isWcBettingSlot);
    }

    private static String teamFifa(Team team) {
        if (team == null) {
            return null;
        }
        return Wc26TeamCatalog.fifaCodeForKnownName(team.getTitle())
                .or(() -> Optional.ofNullable(team.getCountry()).flatMap(Wc26TeamCatalog::fifaCodeForKnownName))
                .orElse(null);
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
