package net.friendly_bets.melbet;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.matchschedule.ExternalCompetitionService;
import net.friendly_bets.matchschedule.MatchdaySlotSupport;
import net.friendly_bets.melbet.client.MelbetHttpClient;
import net.friendly_bets.melbet.client.MelbetHttpFetchResult;
import net.friendly_bets.melbet.config.MelbetProperties;
import net.friendly_bets.melbet.mapping.MelbetBetTitleMapper;
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
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.ExternalDataLayer;
import net.friendly_bets.providers.ExternalProviderIds;
import net.friendly_bets.marathonbet.MarathonbetSyncBatchSupport;
import net.friendly_bets.providers.odds.OddsCronSlotPlan;
import net.friendly_bets.providers.odds.OddsCronSlotPlanner;
import net.friendly_bets.providers.odds.OddsFetchPolicy;
import net.friendly_bets.providers.odds.OddsRefreshSupport;
import net.friendly_bets.providers.odds.OddsSlotScope;
import net.friendly_bets.providers.odds.OddsSlotWindow;
import net.friendly_bets.services.ErrorLogService;
import net.friendly_bets.services.ExternalApiMonitoringService;
import net.friendly_bets.services.AppSettingsService;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.MatchScheduleDisplayService;
import net.friendly_bets.services.MatchScheduleQueryService;
import net.friendly_bets.services.RunningSeasonLookup;
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
public class MelbetSyncService {

    private static final Logger log = LoggerFactory.getLogger(MelbetSyncService.class);

    private final MelbetProperties properties;
    private final MelbetHttpClient httpClient;
    private final MelbetEventMatcher eventMatcher;
    private final MelbetBetTitleMapper betTitleMapper;
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

    private final ReentrantLock pipelineLock = new ReentrantLock();

    /**
     * Manual admin sync.
     * <ul>
     *   <li>{@code force=false}: current matchday only, GetEvent only for matches without odds.</li>
     *   <li>{@code force=true}: given matchday + matchScheduleIds, fetch even if odds exist.</li>
     * </ul>
     */
    public MelbetSyncResult syncSlot(
            String leagueId,
            String season,
            boolean force,
            Integer matchday,
            List<String> matchScheduleIds
    ) {
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() == null) {
            throw new BadRequestException("melbetInvalidTournamentId");
        }
        String code = league.getLeagueCode().name();
        Long tournamentId = properties.tournamentIdForLeague(code);
        if (tournamentId == null || tournamentId <= 0) {
            throw new BadRequestException("melbetInvalidTournamentId");
        }

        String resolvedSeason = resolveSeason(season, league);
        List<Integer> slotOrders;
        List<String> idFilter;
        OddsFetchPolicy policy;
        if (force) {
            if (matchday == null || matchday < 1) {
                throw new BadRequestException("matchdayRequired");
            }
            if (matchScheduleIds == null || matchScheduleIds.isEmpty()) {
                throw new BadRequestException("matchScheduleIdsRequired");
            }
            slotOrders = List.of(matchday);
            idFilter = matchScheduleIds;
            policy = OddsFetchPolicy.FORCE;
        } else {
            ExternalCompetitionInfoDto info = externalCompetitionService.getCompetitionInfoForLeague(
                    league.getId(), resolvedSeason);
            List<Integer> currentOnly = OddsSlotWindow.resolveSlotOrders(
                    info, OddsSlotScope.CURRENT);
            if (currentOnly.isEmpty()) {
                throw new BadRequestException("currentMatchdayUnresolved");
            }
            slotOrders = currentOnly;
            idFilter = null;
            policy = OddsFetchPolicy.MISSING_ONLY;
        }

        pipelineLock.lock();
        try {
            List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
            Instant listRequestedAt = Instant.now();
            MelbetHttpFetchResult listResult = httpClient.fetchTournamentEvents(tournamentId);
            httpLogs.add(toLog("TOURNAMENT", tournamentId, listResult, listRequestedAt));
            if (!listResult.isSuccess()) {
                ExternalApiMonitoringRun failedRun = monitoringService.begin(
                        ExternalDataLayer.ODDS,
                        ExternalProviderIds.MELBET,
                        ExternalApiMonitoringTrigger.ADMIN,
                        code,
                        resolvedSeason
                );
                failedRun.setSlotScope(OddsSlotScope.CURRENT.name());
                failedRun.setSlotOrders(slotOrders);
                finalizeRun(failedRun, httpLogs, false, SlotCounters.empty(), listResult.toErrorKey());
                throw new BadRequestException(listResult.toErrorKey());
            }

            List<MelbetPrematchEvent> prematch = MelbetTournamentParser.parsePrematchEvents(listResult.getBody());
            ExternalApiMonitoringRun run = monitoringService.begin(
                    ExternalDataLayer.ODDS,
                    ExternalProviderIds.MELBET,
                    ExternalApiMonitoringTrigger.ADMIN,
                    code,
                    resolvedSeason
            );
            run.setSlotScope(OddsSlotScope.CURRENT.name());
            run.setSlotOrders(slotOrders);

            SlotCounters counters = syncMatches(
                    league, slotOrders, resolvedSeason, prematch, idFilter, true, policy, false, httpLogs);
            finalizeRun(run, httpLogs, true, counters, null);
            return toResult(code, resolvedSeason, slotOrders, counters, true, null);
        } finally {
            pipelineLock.unlock();
        }
    }

    public MelbetSyncResult syncLeague(String leagueCode) {
        return syncLeague(leagueCode, true);
    }

    public MelbetSyncResult syncLeague(String leagueCode, boolean applyStagePause) {
        if (leagueCode == null || leagueCode.isBlank()) {
            return MelbetSyncResult.builder().build();
        }
        String code = leagueCode.trim().toUpperCase(Locale.ROOT);
        Long tournamentId = properties.tournamentIdForLeague(code);
        if (tournamentId == null || tournamentId <= 0) {
            log.debug("melbet syncLeague skipped: no tournamentId for {}", code);
            return MelbetSyncResult.builder().leagueCode(code).build();
        }

        Optional<Season> active = runningSeasonLookup.findRunningSeason();
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return MelbetSyncResult.builder().leagueCode(code).build();
        }
        League league = findLeagueInSeason(active.get(), code);
        if (league == null) {
            return MelbetSyncResult.builder().leagueCode(code).build();
        }

        pipelineLock.lock();
        try {
            applyJitter();
            return syncLeagueLocked(league, code, tournamentId, active.get(), applyStagePause);
        } finally {
            pipelineLock.unlock();
        }
    }

    private MelbetSyncResult syncLeagueLocked(
            League league,
            String code,
            long tournamentId,
            Season seasonEntity,
            boolean applyStagePause
    ) {
        String season = matchdaySupport.resolveExternalSeasonYear(seasonEntity, league.getLeagueCode());
        ExternalCompetitionInfoDto info = externalCompetitionService.getCompetitionInfoForLeague(
                league.getId(), season);
        OddsCronSlotPlan plan = oddsCronSlotPlanner.plan(league, season, info, Instant.now());
        List<Integer> slotOrders = plan.slotOrders();

        List<ExternalApiHttpLogEntry> httpLogs = new ArrayList<>();
        ExternalApiMonitoringRun run = monitoringService.begin(
                ExternalDataLayer.ODDS,
                ExternalProviderIds.MELBET,
                ExternalApiMonitoringTrigger.CRON,
                code,
                season
        );
        run.setSlotScope(plan.scope() != null ? plan.scope().name() : "SKIP");
        run.setSlotOrders(slotOrders);

        if (plan.skip() || slotOrders.isEmpty() || plan.fetchPolicy() == null) {
            String reason = plan.reason() != null ? plan.reason() : "noSseEligible";
            log.info("melbet syncLeague {}: ODDS cron skip reason={} — no tournament HTTP", code, reason);
            finalizeRun(run, httpLogs, false, SlotCounters.empty(), reason);
            return toResult(code, season, slotOrders, SlotCounters.empty(), true, null);
        }

        Instant listRequestedAt = Instant.now();
        MelbetHttpFetchResult listResult = httpClient.fetchTournamentEvents(tournamentId);
        httpLogs.add(toLog("TOURNAMENT", tournamentId, listResult, listRequestedAt));
        if (!listResult.isSuccess()) {
            String errorSummary = listResult.toErrorKey();
            errorLogService.record(ErrorLogService.Entry.builder()
                    .severity(ErrorLogService.SEVERITY_ERROR)
                    .layer(ExternalDataLayer.ODDS.name())
                    .provider(ExternalProviderIds.MELBET)
                    .code(ErrorLogService.CODE_PROVIDER_FETCH_FAILED)
                    .message(errorSummary)
                    .leagueCode(code)
                    .season(season)
                    .build());
            finalizeRun(run, httpLogs, false, SlotCounters.empty(), errorSummary);
            return toResult(code, season, slotOrders, SlotCounters.empty(), false, errorSummary);
        }

        List<MelbetPrematchEvent> prematch = MelbetTournamentParser.parsePrematchEvents(listResult.getBody());
        SlotCounters counters = syncMatches(
                league,
                slotOrders,
                season,
                prematch,
                null,
                false,
                plan.fetchPolicy(),
                applyStagePause,
                httpLogs
        );

        finalizeRun(run, httpLogs, true, counters, null);
        log.info(
                "melbet syncLeague {}: eligible={}, matched={}, saved={}, events={}, skippedFar={}, noBookie={}, mappingFail={}",
                code,
                counters.matchesEligible(),
                counters.matchesMatched(),
                counters.mergedSaved(),
                counters.eventCalls(),
                counters.skippedFar(),
                counters.skippedNoBookieEvent(),
                counters.mappingFailures()
        );
        return toResult(code, season, slotOrders, counters, true, null);
    }

    private SlotCounters syncMatches(
            League league,
            List<Integer> matchdays,
            String season,
            List<MelbetPrematchEvent> prematch,
            List<String> matchScheduleIds,
            boolean failWhenNoPending,
            OddsFetchPolicy policy,
            boolean applyStagePause,
            List<ExternalApiHttpLogEntry> httpLogs
    ) {
        String leagueCode = league.getLeagueCode().name();
        Instant now = Instant.now();
        Instant fetchedAt = Instant.now();

        List<MatchSchedule> pending = new ArrayList<>();
        for (int matchday : matchdays) {
            List<MatchSchedule> matches = matchScheduleQueryService.getMatches(
                    leagueCode, matchday, season, league.getId());
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
                        continue;
                    }
                    pending.add(match);
                } else {
                    oddsService.freezeIfNeeded(match, now);
                }
            }
        }
        if (pending.isEmpty()) {
            if (failWhenNoPending) {
                throw new BadRequestException("oddsSyncNoMatchdayMatches");
            }
            return SlotCounters.empty();
        }

        List<MatchSchedule> toFetch = new ArrayList<>();
        int skippedFar = 0;
        int refreshWithinHours = appSettingsService.oddsRefreshWithinHours();
        for (MatchSchedule match : pending) {
            if (policy == OddsFetchPolicy.FORCE) {
                toFetch.add(match);
                continue;
            }
            boolean hasOdds = oddsService.findByMatchScheduleId(match.getId())
                    .map(odds -> odds.getMarketGroups() != null && !odds.getMarketGroups().isEmpty())
                    .orElse(false);
            if (policy == OddsFetchPolicy.MISSING_ONLY && hasOdds) {
                skippedFar++;
                continue;
            }
            if (policy == OddsFetchPolicy.REFRESH_WINDOW
                    && !OddsRefreshSupport.needsRefresh(match, hasOdds, now, refreshWithinHours)) {
                skippedFar++;
                continue;
            }
            if (policy == OddsFetchPolicy.REFRESH_WINDOW || policy == OddsFetchPolicy.MISSING_ONLY) {
                toFetch.add(match);
            }
        }

        List<MatchSchedule> sorted = MarathonbetSyncBatchSupport.sortByKickoff(toFetch);
        List<List<MatchSchedule>> stages = MarathonbetSyncBatchSupport.partitionStages(
                sorted, properties.getStageSize());

        int matched = 0;
        int saved = 0;
        int eventCalls = 0;
        int mappingFailures = 0;
        int skippedNoBookieEvent = 0;
        List<String> failedIds = new ArrayList<>();

        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            for (MatchSchedule match : stages.get(stageIndex)) {
                MatchOutcome outcome = syncOneMatch(
                        match, prematch, leagueCode, season, match.getMatchday(), fetchedAt, httpLogs);
                if (outcome.matched()) {
                    matched++;
                }
                if (outcome.saved()) {
                    saved++;
                }
                if (outcome.eventCall()) {
                    eventCalls++;
                }
                if (outcome.noBookieEvent()) {
                    skippedNoBookieEvent++;
                } else if (outcome.hardFail()) {
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

        return new SlotCounters(
                pending.size(), matched, saved, eventCalls, mappingFailures, skippedFar, skippedNoBookieEvent, failedIds);
    }

    private String resolveSeason(String requestedSeason, League league) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        Season active = runningSeasonLookup.findRunningSeasonOrThrow("seasonDatesRequired");
        return matchdaySupport.resolveExternalSeasonYear(active, league.getLeagueCode());
    }

    private MatchOutcome syncOneMatch(
            MatchSchedule match,
            List<MelbetPrematchEvent> prematch,
            String leagueCode,
            String season,
            int matchday,
            Instant fetchedAt,
            List<ExternalApiHttpLogEntry> httpLogs
    ) {
        MelbetEventResolveResult resolve = eventMatcher.resolveAndRecordMappingIssue(
                match, prematch, leagueCode, season, matchday);
        if (!resolve.isMatched()) {
            if (resolve.getMissKind() == MelbetEventResolveResult.MissKind.NO_BOOKIE_EVENT) {
                return MatchOutcome.skippedNoBookie();
            }
            return MatchOutcome.mappingMiss();
        }
        MelbetPrematchEvent event = resolve.getEvent();
        try {
            sleepBeforeEvent();
            Instant requestedAt = Instant.now();
            MelbetHttpFetchResult eventResult = httpClient.fetchEvent(event.getEventId());
            httpLogs.add(toLog("EVENT", event.getEventId(), eventResult, requestedAt));
            if (!eventResult.isSuccess()) {
                sleepRetryAfter(eventResult.getRetryAfterSeconds());
                return MatchOutcome.matchedFail(true);
            }
            List<MappedOddsQuote> quotes = betTitleMapper.mapEventPayload(eventResult.getBody());
            if (quotes.isEmpty()) {
                return MatchOutcome.matchedFail(true);
            }
            OddsMergeResult mergeResult = oddsService.buildAndPersistFromQuotes(
                    match,
                    quotes,
                    List.of(MelbetBookmaker.KEY),
                    fetchedAt,
                    false,
                    null,
                    event.getEventId()
            );
            boolean saved = mergeResult.getMarketGroups() != null && !mergeResult.getMarketGroups().isEmpty();
            return saved ? MatchOutcome.ok(true) : MatchOutcome.matchedFail(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MatchOutcome.matchedFail(false);
        } catch (Exception e) {
            log.warn("melbet sync match failed matchScheduleId={}: {}", match.getId(), e.getMessage());
            return MatchOutcome.matchedFail(false);
        }
    }

    private void finalizeRun(
            ExternalApiMonitoringRun run,
            List<ExternalApiHttpLogEntry> httpLogs,
            boolean tournamentFetched,
            SlotCounters counters,
            String errorSummary
    ) {
        String summary = errorSummary;
        if (counters.mappingFailures() > 0) {
            summary = summary == null
                    ? "mappingFailures=" + counters.mappingFailures()
                    : summary + "; mappingFailures=" + counters.mappingFailures();
        }
        ExternalApiMonitoringCounters monitoringCounters = ExternalApiMonitoringCounters.builder()
                .eligible(counters.matchesEligible())
                .matched(counters.matchesMatched())
                .saved(counters.mergedSaved())
                .sseCalls(counters.eventCalls())
                .mappingFailures(counters.mappingFailures())
                .skipped(counters.skippedFar() + counters.skippedNoBookieEvent())
                .skippedFar(counters.skippedFar())
                .skippedNoBookieEvent(counters.skippedNoBookieEvent())
                .skippedMissingKickoff(0)
                .tournamentFetched(tournamentFetched)
                .build();
        ExternalApiMonitoringStatus status;
        if (errorSummary != null && !tournamentFetched
                && !ExternalApiMonitoringService.isOddsCronSoftSkip(errorSummary)) {
            status = ExternalApiMonitoringStatus.FAILED;
        } else if (ExternalApiMonitoringService.isOddsCronSoftSkip(errorSummary)
                || "noSlots".equals(errorSummary)
                || counters.matchesEligible() == 0) {
            status = ExternalApiMonitoringStatus.SKIPPED;
        } else if (counters.mappingFailures() > 0 || ExternalApiMonitoringService.countFailed(httpLogs) > 0) {
            status = counters.mergedSaved() > 0
                    ? ExternalApiMonitoringStatus.PARTIAL
                    : ExternalApiMonitoringStatus.FAILED;
        } else {
            status = ExternalApiMonitoringStatus.SUCCESS;
        }
        monitoringService.finalizeAndSave(
                run, status, monitoringCounters, httpLogs, counters.failedIds(), summary);
    }

    private static ExternalApiHttpLogEntry toLog(
            String type,
            long targetId,
            MelbetHttpFetchResult result,
            Instant requestedAt
    ) {
        return ExternalApiMonitoringService.httpLog(
                type,
                String.valueOf(targetId),
                result.getHttpStatus(),
                result.getOutcome() != null ? result.getOutcome().name() : null,
                result.getDurationMs(),
                result.getErrorDetail(),
                result.getRetryAfterSeconds(),
                requestedAt
        );
    }

    private static MelbetSyncResult toResult(
            String code,
            String season,
            List<Integer> slotOrders,
            SlotCounters counters,
            boolean tournamentFetched,
            String errorSummary
    ) {
        return MelbetSyncResult.builder()
                .tournamentFetched(tournamentFetched)
                .matchesEligible(counters.matchesEligible())
                .matchesMatched(counters.matchesMatched())
                .mergedSaved(counters.mergedSaved())
                .eventCalls(counters.eventCalls())
                .mappingFailures(counters.mappingFailures())
                .skippedFar(counters.skippedFar())
                .skippedNoBookieEvent(counters.skippedNoBookieEvent())
                .failedMatchScheduleIds(counters.failedIds())
                .leagueCode(code)
                .season(season)
                .slotOrders(slotOrders)
                .errorSummary(errorSummary)
                .build();
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

    private void applyJitter() {
        int minutes = Math.max(0, properties.getSyncJitterMinutes());
        if (minutes <= 0) {
            return;
        }
        long sleepMs = ThreadLocalRandom.current().nextLong(0, minutes * 60_000L + 1);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepBeforeEvent() throws InterruptedException {
        long min = Math.max(0, properties.getEventDelayMinMs());
        long max = Math.max(min, properties.getEventDelayMaxMs());
        long delay = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }

    private void sleepStagePause() {
        int minutes = Math.max(0, properties.getStagePauseMinutes());
        if (minutes <= 0) {
            return;
        }
        long jitter = ThreadLocalRandom.current().nextLong(0, 60_000L);
        try {
            Thread.sleep(minutes * 60_000L + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepRetryAfter(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(seconds, 120) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record SlotCounters(
            int matchesEligible,
            int matchesMatched,
            int mergedSaved,
            int eventCalls,
            int mappingFailures,
            int skippedFar,
            int skippedNoBookieEvent,
            List<String> failedIds
    ) {
        static SlotCounters empty() {
            return new SlotCounters(0, 0, 0, 0, 0, 0, 0, List.of());
        }
    }

    private record MatchOutcome(
            boolean matched,
            boolean saved,
            boolean eventCall,
            boolean noBookieEvent,
            boolean hardFail
    ) {
        static MatchOutcome ok(boolean eventCall) {
            return new MatchOutcome(true, true, eventCall, false, false);
        }

        static MatchOutcome matchedFail(boolean eventCall) {
            return new MatchOutcome(true, false, eventCall, false, true);
        }

        static MatchOutcome mappingMiss() {
            return new MatchOutcome(false, false, false, false, true);
        }

        static MatchOutcome skippedNoBookie() {
            return new MatchOutcome(false, false, false, true, false);
        }
    }
}
