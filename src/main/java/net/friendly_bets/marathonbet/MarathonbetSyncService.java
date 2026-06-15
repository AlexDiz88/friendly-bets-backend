package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.gameresults.ApiSyncIssueService;
import net.friendly_bets.gameresults.ExternalCompetitionService;
import net.friendly_bets.gameresults.MatchdaySlotSupport;
import net.friendly_bets.gameresults.GameResultQueryService;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetRequestType;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.models.marathonbet.MarathonbetHttpLogEntry;
import net.friendly_bets.marathonbet.mapping.MarathonbetBetTitleMapper;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.marathonbet.MarathonbetSyncRun;
import net.friendly_bets.oddsapi.GameResultNotStarted;
import net.friendly_bets.oddsapi.OddsMergedOddsService;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.repositories.MarathonbetSyncRunRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.RunningSeasonLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MarathonbetSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarathonbetSyncService.class);

    private final MarathonbetProperties properties;
    private final MarathonbetTournamentClient tournamentClient;
    private final MarathonbetScrapeService scrapeService;
    private final MarathonbetEventMatcher eventMatcher;
    private final MarathonbetBetTitleMapper betTitleMapper;
    private final OddsMergedOddsService oddsMergedOddsService;
    private final GameResultQueryService gameResultQueryService;
    private final ExternalCompetitionService externalCompetitionService;
    private final MatchdaySlotSupport matchdaySupport;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final ApiSyncIssueService apiSyncIssueService;
    private final MarathonbetSyncRunRepository syncRunRepository;

    public MarathonbetSyncResult runTick() {
        return runTick(MarathonbetSlotScope.BOTH);
    }

    public MarathonbetSyncResult runTick(MarathonbetSlotScope scope) {
        applyJitter();
        if (!properties.isSyncEnabled()) {
            return MarathonbetSyncResult.builder().build();
        }
        Optional<Season> active = runningSeasonLookup.findRunningSeason();
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return MarathonbetSyncResult.builder().build();
        }

        List<MarathonbetHttpLogEntry> httpLogs = new ArrayList<>();
        LocalDateTime startedAt = LocalDateTime.now();
        MarathonbetSyncRun run = MarathonbetSyncRun.builder()
                .startedAt(startedAt)
                .manual(false)
                .slotScope(scope.name())
                .build();

        int eligible = 0;
        int matched = 0;
        int saved = 0;
        int sseCalls = 0;
        int mappingFailures = 0;
        List<String> failedIds = new ArrayList<>();
        List<Integer> allSlots = new ArrayList<>();
        boolean tournamentFetched = false;
        String leagueCode = null;
        String season = null;
        String errorSummary = null;

        for (League league : active.get().getLeagues()) {
            if (league == null || league.getLeagueCode() != League.LeagueCode.WC) {
                continue;
            }
            if (!isPrimaryLeague(league.getLeagueCode().name())) {
                continue;
            }
            Long tournamentId = properties.getTournamentTreeIds().get("WC");
            if (tournamentId == null || tournamentId <= 0) {
                continue;
            }

            leagueCode = league.getLeagueCode().name();
            season = matchdaySupport.resolveExternalSeasonYear(active.get(), league.getLeagueCode());
            ExternalCompetitionInfoDto info = externalCompetitionService.getCompetitionInfoForLeague(
                    league.getId(),
                    season
            );
            List<Integer> slotOrders = MarathonbetSyncSlotWindow.resolveSlotOrders(info, scope);
            allSlots.addAll(slotOrders);
            run.setLeagueCode(leagueCode);
            run.setSeason(season);
            run.setSlotOrders(slotOrders);

            if (slotOrders.isEmpty()) {
                log.debug("marathonbet sync tick skipped: no slots for scope {}", scope);
                continue;
            }

            LocalDateTime tournamentRequestedAt = LocalDateTime.now();
            MarathonbetHttpFetchResult tournamentResult = tournamentClient.fetchTournament(tournamentId);
            httpLogs.add(MarathonbetHttpLogSupport.toLogEntry(
                    tournamentResult,
                    MarathonbetRequestType.TOURNAMENT,
                    tournamentId,
                    tournamentRequestedAt
            ));
            if (!tournamentResult.isSuccess()) {
                errorSummary = tournamentResult.toErrorKey();
                log.warn(
                        "marathonbet tournament fetch failed: status={}, outcome={}, durationMs={}",
                        tournamentResult.getHttpStatus(),
                        tournamentResult.getOutcome(),
                        tournamentResult.getDurationMs()
                );
                apiSyncIssueService.recordMarathonbetFetchFailed(leagueCode, season, errorSummary);
                apiSyncIssueService.recordMarathonbetPrimaryUnavailable(leagueCode, season, errorSummary);
                break;
            }
            tournamentFetched = true;
            JsonNode tournamentRoot = tournamentResult.getBody();

            List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);
            for (int matchday : slotOrders) {
                SlotSyncCounters counters = syncLeagueMatchday(
                        league,
                        matchday,
                        season,
                        prematch,
                        null,
                        false,
                        httpLogs
                );
                eligible += counters.matchesEligible();
                matched += counters.matchesMatched();
                saved += counters.mergedSaved();
                sseCalls += counters.sseCalls();
                mappingFailures += counters.mappingFailures();
                failedIds.addAll(counters.failedGameResultIds());
            }
        }

        finalizeRun(
                run,
                httpLogs,
                tournamentFetched,
                eligible,
                matched,
                saved,
                sseCalls,
                mappingFailures,
                failedIds,
                errorSummary
        );

        log.info(
                "marathonbet sync tick scope={}: fetched={}, eligible={}, matched={}, saved={}, sse={}, httpFail={}/{}",
                scope,
                tournamentFetched,
                eligible,
                matched,
                saved,
                sseCalls,
                run.getHttpRequestsFailed(),
                run.getHttpRequestsTotal()
        );

        return MarathonbetSyncResult.builder()
                .tournamentFetched(tournamentFetched)
                .matchesEligible(eligible)
                .matchesMatched(matched)
                .mergedSaved(saved)
                .sseCalls(sseCalls)
                .mappingFailures(mappingFailures)
                .failedGameResultIds(run.getFailedGameResultIds())
                .leagueCode(leagueCode)
                .season(season)
                .slotOrders(allSlots)
                .errorSummary(errorSummary)
                .build();
    }

    public MarathonbetSyncResult syncSlot(
            String leagueId,
            int matchday,
            String season,
            List<String> gameResultIds
    ) {
        if (!properties.isSyncEnabled()) {
            throw new BadRequestException("marathonbetSyncDisabled");
        }
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() != League.LeagueCode.WC) {
            throw new BadRequestException("marathonbetWcOnly");
        }
        Long tournamentId = properties.getTournamentTreeIds().get("WC");
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }

        String resolvedSeason = resolveSeason(season, league);
        List<MarathonbetHttpLogEntry> httpLogs = new ArrayList<>();
        LocalDateTime tournamentRequestedAt = LocalDateTime.now();
        MarathonbetHttpFetchResult tournamentResult = tournamentClient.fetchTournament(tournamentId);
        httpLogs.add(MarathonbetHttpLogSupport.toLogEntry(
                tournamentResult,
                MarathonbetRequestType.TOURNAMENT,
                tournamentId,
                tournamentRequestedAt
        ));
        if (!tournamentResult.isSuccess()) {
            throw new BadRequestException(tournamentResult.toErrorKey());
        }
        JsonNode tournamentRoot = tournamentResult.getBody();
        List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);

        MarathonbetSyncRun run = MarathonbetSyncRun.builder()
                .startedAt(LocalDateTime.now())
                .manual(true)
                .slotScope(MarathonbetSlotScope.BOTH.name())
                .leagueCode(league.getLeagueCode().name())
                .season(resolvedSeason)
                .slotOrders(List.of(matchday))
                .tournamentFetched(true)
                .build();

        SlotSyncCounters counters = syncLeagueMatchday(
                league,
                matchday,
                resolvedSeason,
                prematch,
                gameResultIds,
                true,
                httpLogs
        );

        finalizeRun(
                run,
                httpLogs,
                true,
                counters.matchesEligible(),
                counters.matchesMatched(),
                counters.mergedSaved(),
                counters.sseCalls(),
                counters.mappingFailures(),
                counters.failedGameResultIds(),
                null
        );

        return MarathonbetSyncResult.builder()
                .tournamentFetched(true)
                .matchesEligible(counters.matchesEligible())
                .matchesMatched(counters.matchesMatched())
                .mergedSaved(counters.mergedSaved())
                .sseCalls(counters.sseCalls())
                .mappingFailures(counters.mappingFailures())
                .failedGameResultIds(counters.failedGameResultIds())
                .leagueCode(league.getLeagueCode().name())
                .season(resolvedSeason)
                .slotOrders(List.of(matchday))
                .build();
    }

    public Optional<MarathonbetSyncRun> findLatestRun() {
        return syncRunRepository.findFirstByOrderByStartedAtDesc();
    }

    public List<MarathonbetSyncRun> findRecentRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return syncRunRepository.findByStartedAtAfterOrderByStartedAtDesc(
                LocalDateTime.now().minusDays(30),
                PageRequest.of(0, safeLimit)
        );
    }

    private SlotSyncCounters syncLeagueMatchday(
            League league,
            int matchday,
            String season,
            List<MarathonbetPrematchEvent> prematch,
            List<String> gameResultIds,
            boolean failWhenNoPending,
            List<MarathonbetHttpLogEntry> httpLogs
    ) {
        String leagueCode = league.getLeagueCode().name();
        List<GameResultRecord> matches = gameResultQueryService.getMatches(
                leagueCode,
                matchday,
                season,
                league.getId()
        );
        if (gameResultIds != null && !gameResultIds.isEmpty()) {
            Set<String> allowed = new HashSet<>(gameResultIds);
            matches = matches.stream()
                    .filter(m -> m.getId() != null && allowed.contains(m.getId()))
                    .toList();
        }

        LocalDateTime now = GameResultNotStarted.nowUtc();
        List<GameResultRecord> pending = new ArrayList<>();
        for (GameResultRecord match : matches) {
            if (GameResultNotStarted.isNotStarted(match, now)) {
                pending.add(match);
            } else {
                oddsMergedOddsService.freezeIfNeeded(match, now);
            }
        }

        if (pending.isEmpty()) {
            if (failWhenNoPending) {
                throw new BadRequestException("oddsSyncNoMatchdayMatches");
            }
            return new SlotSyncCounters(0, 0, 0, 0, 0, List.of());
        }

        int matched = 0;
        int saved = 0;
        int sseCalls = 0;
        int failures = 0;
        List<String> failedIds = new ArrayList<>();

        for (GameResultRecord match : pending) {
            Optional<MarathonbetPrematchEvent> eventOpt = eventMatcher.resolveAndPersistTreeId(
                    match,
                    prematch,
                    leagueCode,
                    season,
                    matchday
            );
            if (eventOpt.isEmpty()) {
                failures++;
                if (match.getId() != null) {
                    failedIds.add(match.getId());
                }
                continue;
            }
            matched++;
            MarathonbetPrematchEvent event = eventOpt.get();
            try {
                sleepBeforeSse();
                LocalDateTime sseRequestedAt = LocalDateTime.now();
                MarathonbetHttpFetchResult sseResult = scrapeService.fetchEventSnapshotResult(event.getTreeId());
                httpLogs.add(MarathonbetHttpLogSupport.toLogEntry(
                        sseResult,
                        MarathonbetRequestType.SSE,
                        event.getTreeId(),
                        sseRequestedAt
                ));
                if (!sseResult.isSuccess()) {
                    log.warn(
                            "marathonbet SSE failed treeId={}: status={}, outcome={}",
                            event.getTreeId(),
                            sseResult.getHttpStatus(),
                            sseResult.getOutcome()
                    );
                    failures++;
                    failedIds.add(match.getId());
                    continue;
                }
                JsonNode eventRoot = sseResult.getBody();
                sseCalls++;
                MarathonbetExtractedMarkets extracted = MarathonbetMarketExtractor.extractAll(eventRoot);
                List<MappedOddsQuote> quotes = betTitleMapper.map(
                        extracted,
                        event.getHomeTeam(),
                        event.getAwayTeam()
                );
                if (quotes.isEmpty()) {
                    failures++;
                    failedIds.add(match.getId());
                    continue;
                }
                OddsMergeResult mergeResult = oddsMergedOddsService.buildAndPersistFromQuotes(
                        match,
                        quotes,
                        List.of(MarathonbetBookmaker.KEY),
                        now,
                        false
                );
                if (mergeResult.getMarketGroups() != null && !mergeResult.getMarketGroups().isEmpty()) {
                    saved++;
                } else {
                    failures++;
                    failedIds.add(match.getId());
                }
                log.debug("marathonbet SSE snapshot for treeId={}", event.getTreeId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failures++;
                failedIds.add(match.getId());
            } catch (Exception e) {
                log.warn("marathonbet sync match failed gameResultId={}: {}", match.getId(), e.getMessage());
                failures++;
                if (match.getId() != null) {
                    failedIds.add(match.getId());
                }
            }
        }

        return new SlotSyncCounters(
                pending.size(),
                matched,
                saved,
                sseCalls,
                failures,
                failedIds
        );
    }

    private boolean isPrimaryLeague(String leagueCode) {
        return properties.getPrimaryForLeagues() != null
                && properties.getPrimaryForLeagues().stream()
                .anyMatch(code -> code != null && code.equalsIgnoreCase(leagueCode));
    }

    private String resolveSeason(String requestedSeason, League league) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        Season active = runningSeasonLookup.findRunningSeasonOrThrow("seasonDatesRequired");
        return matchdaySupport.resolveExternalSeasonYear(active, league.getLeagueCode());
    }

    private void finalizeRun(
            MarathonbetSyncRun run,
            List<MarathonbetHttpLogEntry> httpLogs,
            boolean tournamentFetched,
            int eligible,
            int matched,
            int saved,
            int sseCalls,
            int mappingFailures,
            List<String> failedIds,
            String errorSummary
    ) {
        run.setTournamentFetched(tournamentFetched);
        run.setMatchesEligible(eligible);
        run.setMatchesMatched(matched);
        run.setMergedSaved(saved);
        run.setSseCalls(sseCalls);
        run.setMappingFailures(mappingFailures);
        run.setFailedGameResultIds(new ArrayList<>(new LinkedHashSet<>(failedIds)));
        run.setErrorSummary(errorSummary);
        run.setHttpLogs(httpLogs);
        run.setHttpRequestsTotal(httpLogs.size());
        run.setHttpRequestsFailed(MarathonbetHttpLogSupport.countFailed(httpLogs));
        LocalDateTime finishedAt = LocalDateTime.now();
        run.setFinishedAt(finishedAt);
        if (run.getStartedAt() != null) {
            run.setDurationMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        }
        syncRunRepository.save(run);
    }

    private void sleepBeforeSse() throws InterruptedException {
        long min = properties.getSseDelayMinMs();
        long max = properties.getSseDelayMaxMs();
        if (max < min) {
            max = min;
        }
        long delayMs = min == max
                ? min
                : ThreadLocalRandom.current().nextLong(min, max + 1);
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    private void applyJitter() {
        int minutes = properties.getSyncJitterMinutes();
        if (minutes <= 0) {
            return;
        }
        int delayMs = ThreadLocalRandom.current().nextInt(0, minutes * 60_000 + 1);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record SlotSyncCounters(
            int matchesEligible,
            int matchesMatched,
            int mergedSaved,
            int sseCalls,
            int mappingFailures,
            List<String> failedGameResultIds
    ) {
    }
}
