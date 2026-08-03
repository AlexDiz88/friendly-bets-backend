package net.friendly_bets.marathonbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.matchschedule.ExternalCompetitionService;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.marathonbet.client.MarathonbetHttpFetchResult;
import net.friendly_bets.marathonbet.client.MarathonbetRequestType;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
import net.friendly_bets.marathonbet.mapping.MarathonbetBetTitleMapper;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.monitoring.ExternalApiHttpLogEntry;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringCounters;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringRun;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringStatus;
import net.friendly_bets.models.monitoring.ExternalApiMonitoringTrigger;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.odds.MatchScheduleNotStarted;
import net.friendly_bets.odds.OddsService;
import net.friendly_bets.odds.mapping.MappedOddsQuote;
import net.friendly_bets.odds.mapping.OddsMergeResult;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.AppSettingsService;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.RunningSeasonLookup;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.odds.OddsCronSlotPlan;
import net.friendly_bets.providers.odds.OddsCronSlotPlanner;
import net.friendly_bets.providers.odds.OddsFetchPolicy;
import net.friendly_bets.providers.odds.OddsRefreshSupport;
import net.friendly_bets.providers.odds.OddsSlotScope;
import net.friendly_bets.providers.odds.OddsSlotWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class MarathonbetSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarathonbetSyncService.class);

    private final MarathonbetProperties properties;
    private final MarathonbetTournamentClient tournamentClient;
    private final MarathonbetScrapeService scrapeService;
    private final MarathonbetEventMatcher eventMatcher;
    private final MarathonbetBetTitleMapper betTitleMapper;
    private final OddsService oddsService;
    private final MatchScheduleQueryService matchScheduleQueryService;
    private final ExternalCompetitionService externalCompetitionService;
    private final MatchdaySlotSupport matchdaySupport;
    private final RunningSeasonLookup runningSeasonLookup;
    private final GetEntityService getEntityService;
    private final ErrorLogService errorLogService;
    private final ExternalApiMonitoringService monitoringService;
    private final OddsCronSlotPlanner oddsCronSlotPlanner;
    private final AppSettingsService appSettingsService;

    /** Ensures only one league runs listing+SSE at a time. */
    private final ReentrantLock pipelineLock = new ReentrantLock();

    /**
     * Scheduled per-league sync (ODDS cron slot plan), with stage batching and refresh policy.
     */
    public MarathonbetSyncResult syncLeague(String leagueCode) {
        return syncLeague(leagueCode, true);
    }

    public MarathonbetSyncResult syncLeague(String leagueCode, boolean applyStagePause) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return MarathonbetSyncResult.builder().build();
        }
        String code = leagueCode.trim().toUpperCase(Locale.ROOT);
        Long tournamentId = properties.tournamentTreeIdForLeague(code);
        if (tournamentId == null || tournamentId <= 0) {
            log.debug("marathonbet syncLeague skipped: no tournamentTreeId for {}", code);
            return MarathonbetSyncResult.builder().leagueCode(code).build();
        }

        Optional<Season> active = runningSeasonLookup.findRunningSeason();
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return MarathonbetSyncResult.builder().leagueCode(code).build();
        }
        League league = findLeagueInSeason(active.get(), code);
        if (league == null) {
            log.debug("marathonbet syncLeague skipped: league {} not in active season", code);
            return MarathonbetSyncResult.builder().leagueCode(code).build();
        }

        pipelineLock.lock();
        try {
            applyJitter();
            return syncLeagueLocked(league, code, tournamentId, active.get(), applyStagePause, false);
        } finally {
            pipelineLock.unlock();
        }
    }

    /** Legacy entry: sync every configured league in the active season (sequential, mutex held per league). */
    public MarathonbetSyncResult runTick() {
        return runTick(OddsSlotScope.BOTH);
    }

    public MarathonbetSyncResult runTick(OddsSlotScope scope) {
        Optional<Season> active = runningSeasonLookup.findRunningSeason();
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return MarathonbetSyncResult.builder().build();
        }
        MarathonbetSyncResult last = MarathonbetSyncResult.builder().build();
        for (League league : active.get().getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            String code = league.getLeagueCode().name();
            if (properties.tournamentTreeIdForLeague(code) == null) {
                continue;
            }
            last = syncLeague(code, true);
        }
        return last;
    }

    /**
     * Manual admin sync.
     * <ul>
     *   <li>{@code force=false}: current matchday only, SSE only for matches without odds.</li>
     *   <li>{@code force=true}: given matchday + matchScheduleIds, SSE even if odds exist.</li>
     * </ul>
     */
    public MarathonbetSyncResult syncSlot(
            String leagueId,
            String season,
            boolean force,
            Integer matchday,
            List<String> matchScheduleIds
    ) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() == null) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }
        String code = league.getLeagueCode().name();
        Long tournamentId = properties.tournamentTreeIdForLeague(code);
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("marathonbetInvalidTournamentId");
        }

        String resolvedSeason = resolveSeason(season, league);
        List<Integer> slotOrders;
        List<String> idFilter;
        OddsFetchPolicy ssePolicy;
        if (force) {
            if (matchday == null || matchday < 1) {
                throw new BadRequestException("matchdayRequired");
            }
            if (matchScheduleIds == null || matchScheduleIds.isEmpty()) {
                throw new BadRequestException("matchScheduleIdsRequired");
            }
            slotOrders = List.of(matchday);
            idFilter = matchScheduleIds;
            ssePolicy = OddsFetchPolicy.FORCE;
        } else {
            ExternalCompetitionInfoDto info = externalCompetitionService.getCompetitionInfoForLeague(
                    league.getId(),
                    resolvedSeason
            );
            List<Integer> currentOnly = OddsSlotWindow.resolveSlotOrders(
                    info, OddsSlotScope.CURRENT);
            if (currentOnly.isEmpty()) {
                throw new BadRequestException("currentMatchdayUnresolved");
            }
            slotOrders = currentOnly;
            idFilter = null;
            ssePolicy = OddsFetchPolicy.MISSING_ONLY;
        }

        pipelineLock.lock();
        try {
            List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
            Instant tournamentRequestedAt = Instant.now();
            MarathonbetHttpFetchResult tournamentResult = tournamentClient.fetchTournament(tournamentId);
            httpLogs.add(MarathonbetHttpLogSupport.toLogEntry(
                    tournamentResult,
                    MarathonbetRequestType.TOURNAMENT,
                    tournamentId,
                    tournamentRequestedAt
            ));
            if (!tournamentResult.isSuccess()) {
                ExternalApiMonitoringRun failedRun = monitoringService.begin(
                        ExternalDataLayer.ODDS,
                        "marathonbet",
                        ExternalApiMonitoringTrigger.ADMIN,
                        code,
                        resolvedSeason
                );
                failedRun.setSlotScope(OddsSlotScope.CURRENT.name());
                failedRun.setSlotOrders(slotOrders);
                finalizeOddsRun(
                        failedRun,
                        httpLogs,
                        false,
                        SlotSyncCounters.empty(),
                        tournamentResult.toErrorKey()
                );
                throw new BadRequestException(tournamentResult.toErrorKey());
            }
            List<MarathonbetPrematchEvent> prematch =
                    MarathonbetTournamentParser.parsePrematchEvents(tournamentResult.getBody());

            ExternalApiMonitoringRun run = monitoringService.begin(
                    ExternalDataLayer.ODDS,
                    "marathonbet",
                    ExternalApiMonitoringTrigger.ADMIN,
                    code,
                    resolvedSeason
            );
            run.setSlotScope(OddsSlotScope.CURRENT.name());
            run.setSlotOrders(slotOrders);

            SlotSyncCounters counters = syncMatches(
                    league,
                    slotOrders,
                    resolvedSeason,
                    prematch,
                    idFilter,
                    true,
                    ssePolicy,
                    false,
                    httpLogs
            );

            finalizeOddsRun(run, httpLogs, true, counters, null);

            return toResult(code, resolvedSeason, slotOrders, counters, true, null);
        } finally {
            pipelineLock.unlock();
        }
    }

    private MarathonbetSyncResult syncLeagueLocked(
            League league,
            String code,
            long tournamentId,
            Season seasonEntity,
            boolean applyStagePause,
            boolean failWhenNoPending
    ) {
        String season = matchdaySupport.resolveExternalSeasonYear(seasonEntity, league.getLeagueCode());
        ExternalCompetitionInfoDto info = externalCompetitionService.getCompetitionInfoForLeague(
                league.getId(),
                season
        );
        OddsCronSlotPlan plan = oddsCronSlotPlanner.plan(league, season, info, Instant.now());
        List<Integer> slotOrders = plan.slotOrders();

        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.ODDS,
                "marathonbet",
                ExternalApiMonitoringTrigger.CRON,
                code,
                season
        );
        run.setSlotScope(plan.scope() != null ? plan.scope().name() : "SKIP");
        run.setSlotOrders(slotOrders);

        if (plan.skip() || slotOrders.isEmpty() || plan.fetchPolicy() == null) {
            String reason = plan.reason() != null ? plan.reason() : "noSseEligible";
            log.info("marathonbet syncLeague {}: ODDS cron skip reason={} — no tournament HTTP", code, reason);
            finalizeOddsRun(run, httpLogs, true, SlotSyncCounters.empty(), reason);
            return toResult(code, season, slotOrders, SlotSyncCounters.empty(), true, null);
        }

        Instant tournamentRequestedAt = Instant.now();
        MarathonbetHttpFetchResult tournamentResult = tournamentClient.fetchTournament(tournamentId);
        httpLogs.add(MarathonbetHttpLogSupport.toLogEntry(
                tournamentResult,
                MarathonbetRequestType.TOURNAMENT,
                tournamentId,
                tournamentRequestedAt
        ));
        if (!tournamentResult.isSuccess()) {
            String errorSummary = tournamentResult.toErrorKey();
            log.warn(
                    "marathonbet tournament fetch failed league={}: status={}, outcome={}",
                    code,
                    tournamentResult.getHttpStatus(),
                    tournamentResult.getOutcome()
            );
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(ErrorLogService.SEVERITY_ERROR)
                    .layer(ExternalDataLayer.ODDS.name())
                    .provider("marathonbet")
                    .code(ErrorLogService.CODE_PROVIDER_FETCH_FAILED)
                    .message(errorSummary)
                    .leagueCode(code)
                    .season(season)
                    .build());
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(ErrorLogService.SEVERITY_ERROR)
                    .layer(ExternalDataLayer.ODDS.name())
                    .provider("marathonbet")
                    .providerRole(ErrorLogService.ROLE_PRIMARY)
                    .code(ErrorLogService.CODE_PRIMARY_UNAVAILABLE)
                    .message(errorSummary)
                    .leagueCode(code)
                    .season(season)
                    .build());
            finalizeOddsRun(run, httpLogs, false, SlotSyncCounters.empty(), errorSummary);
            return toResult(code, season, slotOrders, SlotSyncCounters.empty(), false, errorSummary);
        }

        List<MarathonbetPrematchEvent> prematch =
                MarathonbetTournamentParser.parsePrematchEvents(tournamentResult.getBody());

        SlotSyncCounters counters = syncMatches(
                league,
                slotOrders,
                season,
                prematch,
                null,
                failWhenNoPending,
                plan.fetchPolicy(),
                applyStagePause,
                httpLogs
        );

        finalizeOddsRun(run, httpLogs, true, counters, null);

        log.info(
                "marathonbet syncLeague {}: eligible={}, matched={}, saved={}, sse={}, skippedFar={}, noBookie={}, mappingFail={}, stages paused={}",
                code,
                counters.matchesEligible(),
                counters.matchesMatched(),
                counters.mergedSaved(),
                counters.sseCalls(),
                counters.skippedFar(),
                counters.skippedNoBookieEvent(),
                counters.mappingFailures(),
                applyStagePause
        );

        return toResult(code, season, slotOrders, counters, true, null);
    }

    private SlotSyncCounters syncMatches(
            League league,
            List<Integer> matchdays,
            String season,
            List<MarathonbetPrematchEvent> prematch,
            List<String> matchScheduleIds,
            boolean failWhenNoPending,
            OddsFetchPolicy ssePolicy,
            boolean applyStagePause,
            List<ExternalApiHttpLogEntry> httpLogs
    ) {
        String leagueCode = league.getLeagueCode().name();
        Instant now = Instant.now();
        Instant fetchedAt = Instant.now();

        List<MatchSchedule> pending = new ArrayList<>();
        int skippedMissingKickoff = 0;
        for (int matchday : matchdays) {
            List<MatchSchedule> matches = matchScheduleQueryService.getMatches(
                    leagueCode,
                    matchday,
                    season,
                    league.getId()
            );
            if (matchScheduleIds != null && !matchScheduleIds.isEmpty()) {
                Set<String> allowed = new HashSet<>(matchScheduleIds);
                matches = matches.stream()
                        .filter(m -> m.getId() != null && allowed.contains(m.getId()))
                        .toList();
            }
            for (MatchSchedule match : matches) {
                if (MatchScheduleDisplayService.isFinalized(match)) {
                    oddsService.deleteIfFinalized(match);
                    continue;
                }
                if (MatchScheduleNotStarted.isNotStarted(match, now)) {
                    if (match.getUtcKickoff() == null) {
                        skippedMissingKickoff++;
                        continue;
                    }
                    pending.add(match);
                } else {
                    oddsService.freezeIfNeeded(match, now);
                }
            }
        }

        if (pending.isEmpty()) {
            if (skippedMissingKickoff > 0) {
                return new SlotSyncCounters(0, 0, 0, 0, 0, 0, 0, skippedMissingKickoff, List.of());
            }
            if (failWhenNoPending) {
                throw new BadRequestException("oddsSyncNoMatchdayMatches");
            }
            return SlotSyncCounters.empty();
        }

        List<MatchSchedule> toFetch = new ArrayList<>();
        int skippedFar = 0;
        int refreshWithinHours = appSettingsService.oddsRefreshWithinHours();
        for (MatchSchedule match : pending) {
            if (ssePolicy == OddsFetchPolicy.FORCE) {
                toFetch.add(match);
                continue;
            }
            boolean hasOdds = oddsService.findByMatchScheduleId(match.getId())
                    .map(odds -> odds.getMarketGroups() != null && !odds.getMarketGroups().isEmpty())
                    .orElse(false);
            if (ssePolicy == OddsFetchPolicy.MISSING_ONLY) {
                if (hasOdds) {
                    skippedFar++;
                    continue;
                }
                toFetch.add(match);
                continue;
            }
            // REFRESH_WINDOW (cron)
            if (!OddsRefreshSupport.needsRefresh(match, hasOdds, now, refreshWithinHours)) {
                skippedFar++;
                continue;
            }
            toFetch.add(match);
        }

        List<MatchSchedule> sorted = MarathonbetSyncBatchSupport.sortByKickoff(toFetch);
        List<List<MatchSchedule>> stages = MarathonbetSyncBatchSupport.partitionStages(
                sorted,
                properties.getStageSize()
        );

        int matched = 0;
        int saved = 0;
        int sseCalls = 0;
        int mappingFailures = 0;
        int skippedNoBookieEvent = 0;
        List<String> failedIds = new ArrayList<>();

        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            List<MatchSchedule> stage = stages.get(stageIndex);
            log.debug("marathonbet league={} stage={}/{} size={}", leagueCode, stageIndex + 1, stages.size(), stage.size());
            for (MatchSchedule match : stage) {
                MatchSyncOutcome outcome = syncOneMatch(
                        match,
                        prematch,
                        leagueCode,
                        season,
                        match.getMatchday(),
                        fetchedAt,
                        httpLogs
                );
                matched += outcome.matched() ? 1 : 0;
                saved += outcome.saved() ? 1 : 0;
                sseCalls += outcome.sseCall() ? 1 : 0;
                if (outcome.noBookieEvent()) {
                    skippedNoBookieEvent++;
                } else if (outcome.hardFailure()) {
                    mappingFailures++;
                    if (match.getId() != null) {
                        failedIds.add(match.getId());
                    }
                }
            }
            if (applyStagePause && stageIndex < stages.size() - 1) {
                sleepStagePause();
            }
        }

        return new SlotSyncCounters(
                pending.size(),
                matched,
                saved,
                sseCalls,
                mappingFailures,
                skippedFar,
                skippedNoBookieEvent,
                skippedMissingKickoff,
                failedIds
        );
    }

    private MatchSyncOutcome syncOneMatch(
            MatchSchedule match,
            List<MarathonbetPrematchEvent> prematch,
            String leagueCode,
            String season,
            int matchday,
            Instant fetchedAt,
            List<ExternalApiHttpLogEntry> httpLogs
    ) {
        MarathonbetEventResolveResult resolveResult = eventMatcher.resolveAndRecordMappingIssue(
                match,
                prematch,
                leagueCode,
                season,
                matchday
        );
        if (!resolveResult.isMatched()) {
            if (resolveResult.getMissKind() == MarathonbetEventResolveResult.MissKind.NO_BOOKIE_EVENT) {
                return MatchSyncOutcome.missNoBookie();
            }
            return MatchSyncOutcome.mappingMiss();
        }
        MarathonbetPrematchEvent event = resolveResult.getEvent();
        try {
            sleepBeforeSse();
            Instant sseRequestedAt = Instant.now();
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
                sleepRetryAfter(sseResult.getRetryAfterSeconds());
                return MatchSyncOutcome.matchedFailed();
            }
            MarathonbetExtractedMarkets extracted = MarathonbetMarketExtractor.extractAll(sseResult.getBody());
            List<MappedOddsQuote> quotes = betTitleMapper.map(
                    extracted,
                    event.getHomeTeam(),
                    event.getAwayTeam()
            );
            if (quotes.isEmpty()) {
                return MatchSyncOutcome.matchedFailedSse();
            }
            OddsMergeResult mergeResult = oddsService.buildAndPersistFromQuotes(
                    match,
                    quotes,
                    List.of(MarathonbetBookmaker.KEY),
                    fetchedAt,
                    false,
                    event.getTreeId()
            );
            boolean saved = mergeResult.getMarketGroups() != null && !mergeResult.getMarketGroups().isEmpty();
            return saved ? MatchSyncOutcome.ok() : MatchSyncOutcome.matchedFailedSse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MatchSyncOutcome.matchedFailed();
        } catch (Exception e) {
            log.warn("marathonbet sync match failed matchScheduleId={}: {}", match.getId(), e.getMessage());
            return MatchSyncOutcome.matchedFailed();
        }
    }

    private static League findLeagueInSeason(Season season, String leagueCode) {
        for (League league : season.getLeagues()) {
            if (league != null
                    && league.getLeagueCode() != null
                    && leagueCode.equalsIgnoreCase(league.getLeagueCode().name())) {
                return league;
            }
        }
        return null;
    }

    private String resolveSeason(String requestedSeason, League league) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        Season active = runningSeasonLookup.findRunningSeasonOrThrow("seasonDatesRequired");
        return matchdaySupport.resolveExternalSeasonYear(active, league.getLeagueCode());
    }

    private void finalizeOddsRun(
            ExternalApiMonitoringRun run,
            List<ExternalApiHttpLogEntry> httpLogs,
            boolean tournamentFetched,
            SlotSyncCounters counters,
            String errorSummary
    ) {
        int eligible = counters.matchesEligible();
        int matched = counters.matchesMatched();
        int saved = counters.mergedSaved();
        int sseCalls = counters.sseCalls();
        int mappingFailures = counters.mappingFailures();
        int skippedFar = counters.skippedFar();
        int skippedNoBookie = counters.skippedNoBookieEvent();
        int skippedMissingKickoff = counters.skippedMissingKickoff();
        List<String> failedIds = counters.failedMatchScheduleIds();

        String summary = errorSummary;
        if (mappingFailures > 0) {
            summary = summary == null
                    ? "mappingFailures=" + mappingFailures
                    : summary + "; mappingFailures=" + mappingFailures;
        }
        if (skippedMissingKickoff > 0) {
            String missing = ExternalApiMonitoringService.reasonMissingUtcKickoff(skippedMissingKickoff);
            summary = summary == null ? missing : summary + "; " + missing;
        }
        // Soft skips: far / noBookie / missingKickoff (also warning in errorSummary).
        ExternalApiMonitoringCounters monitoringCounters = ExternalApiMonitoringCounters.builder()
                .eligible(eligible)
                .matched(matched)
                .saved(saved)
                .sseCalls(sseCalls)
                .mappingFailures(mappingFailures)
                .skipped(skippedFar + skippedNoBookie + skippedMissingKickoff)
                .skippedFar(skippedFar)
                .skippedNoBookieEvent(skippedNoBookie)
                .skippedMissingKickoff(skippedMissingKickoff)
                .tournamentFetched(tournamentFetched)
                .build();
        ExternalApiMonitoringStatus status;
        if (errorSummary != null && !tournamentFetched && eligible == 0 && matched == 0 && saved == 0
                && !"noSlots".equals(errorSummary)
                && !errorSummary.startsWith(ExternalApiMonitoringService.REASON_MISSING_UTC_KICKOFF)) {
            status = ExternalApiMonitoringStatus.FAILED;
        } else if ("noSlots".equals(errorSummary)
                || (eligible == 0 && tournamentFetched)
                || (eligible == 0 && skippedMissingKickoff > 0 && mappingFailures == 0)) {
            status = ExternalApiMonitoringStatus.SKIPPED;
        } else if (mappingFailures > 0 || ExternalApiMonitoringService.countFailed(httpLogs) > 0) {
            status = saved > 0 ? ExternalApiMonitoringStatus.PARTIAL : ExternalApiMonitoringStatus.FAILED;
        } else {
            status = ExternalApiMonitoringStatus.SUCCESS;
        }
        monitoringService.finalizeAndSave(run, status, monitoringCounters, httpLogs, failedIds, summary);
    }

    private MarathonbetSyncResult toResult(
            String code,
            String season,
            List<Integer> slotOrders,
            SlotSyncCounters counters,
            boolean tournamentFetched,
            String errorSummary
    ) {
        return MarathonbetSyncResult.builder()
                .tournamentFetched(tournamentFetched)
                .matchesEligible(counters.matchesEligible())
                .matchesMatched(counters.matchesMatched())
                .mergedSaved(counters.mergedSaved())
                .sseCalls(counters.sseCalls())
                .mappingFailures(counters.mappingFailures())
                .skippedFar(counters.skippedFar())
                .skippedNoBookieEvent(counters.skippedNoBookieEvent())
                .failedMatchScheduleIds(counters.failedMatchScheduleIds())
                .leagueCode(code)
                .season(season)
                .slotOrders(slotOrders)
                .errorSummary(errorSummary)
                .build();
    }

    private void sleepRetryAfter(Integer retryAfterSeconds) {
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
            return;
        }
        long delayMs = Math.min(retryAfterSeconds, 120) * 1_000L;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private void sleepStagePause() {
        int minutes = Math.max(0, properties.getStagePauseMinutes());
        if (minutes <= 0) {
            return;
        }
        long baseMs = minutes * 60_000L;
        long jitterMs = ThreadLocalRandom.current().nextLong(0, 60_000L);
        try {
            Thread.sleep(baseMs + jitterMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
            int skippedFar,
            int skippedNoBookieEvent,
            int skippedMissingKickoff,
            List<String> failedMatchScheduleIds
    ) {
        static SlotSyncCounters empty() {
            return new SlotSyncCounters(0, 0, 0, 0, 0, 0, 0, 0, List.of());
        }
    }

    private record MatchSyncOutcome(
            boolean matched,
            boolean saved,
            boolean sseCall,
            boolean hardFailure,
            boolean noBookieEvent
    ) {
        static MatchSyncOutcome ok() {
            return new MatchSyncOutcome(true, true, true, false, false);
        }

        static MatchSyncOutcome missNoBookie() {
            return new MatchSyncOutcome(false, false, false, false, true);
        }

        static MatchSyncOutcome mappingMiss() {
            return new MatchSyncOutcome(false, false, false, true, false);
        }

        static MatchSyncOutcome matchedFailed() {
            return new MatchSyncOutcome(true, false, false, true, false);
        }

        static MatchSyncOutcome matchedFailedSse() {
            return new MatchSyncOutcome(true, false, true, true, false);
        }
    }
}
