package net.friendly_bets.marathonbet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.footballdata.FootballDataSyncService;
import net.friendly_bets.marathonbet.client.MarathonbetTournamentClient;
import net.friendly_bets.marathonbet.config.MarathonbetProperties;
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
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.GetEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final FootballDataSyncService footballDataSyncService;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final SeasonsRepository seasonsRepository;
    private final GetEntityService getEntityService;
    private final ApiSyncIssueService apiSyncIssueService;
    private final MarathonbetSyncRunRepository syncRunRepository;

    public MarathonbetSyncResult runTick() {
        applyJitter();
        if (!properties.isSyncEnabled()) {
            return MarathonbetSyncResult.builder().build();
        }
        Optional<Season> active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return MarathonbetSyncResult.builder().build();
        }

        MarathonbetSyncRun run = MarathonbetSyncRun.builder()
                .startedAt(LocalDateTime.now())
                .manual(false)
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
            season = matchdaySupport.resolveFootballDataSeasonYear(active.get(), league.getLeagueCode());
            ExternalCompetitionInfoDto info = footballDataCompetitionService.getCompetitionInfoForLeague(
                    league.getId(),
                    season
            );
            List<Integer> slotOrders = MarathonbetSyncSlotWindow.resolveSlotOrders(info);
            allSlots.addAll(slotOrders);
            run.setLeagueCode(leagueCode);
            run.setSeason(season);
            run.setSlotOrders(slotOrders);

            JsonNode tournamentRoot;
            try {
                tournamentRoot = tournamentClient.fetchTournament(tournamentId);
                tournamentFetched = true;
            } catch (BadRequestException e) {
                errorSummary = e.getMessage();
                apiSyncIssueService.recordMarathonbetFetchFailed(leagueCode, season, e.getMessage());
                apiSyncIssueService.recordMarathonbetPrimaryUnavailable(leagueCode, season, e.getMessage());
                break;
            }

            List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);
            for (int matchday : slotOrders) {
                SlotSyncCounters counters = syncLeagueMatchday(
                        league,
                        matchday,
                        season,
                        prematch,
                        null,
                        false
                );
                eligible += counters.matchesEligible();
                matched += counters.matchesMatched();
                saved += counters.mergedSaved();
                sseCalls += counters.sseCalls();
                mappingFailures += counters.mappingFailures();
                failedIds.addAll(counters.failedGameResultIds());
            }
        }

        run.setTournamentFetched(tournamentFetched);
        run.setMatchesEligible(eligible);
        run.setMatchesMatched(matched);
        run.setMergedSaved(saved);
        run.setSseCalls(sseCalls);
        run.setMappingFailures(mappingFailures);
        run.setFailedGameResultIds(new ArrayList<>(new LinkedHashSet<>(failedIds)));
        run.setErrorSummary(errorSummary);
        run.setFinishedAt(LocalDateTime.now());
        syncRunRepository.save(run);

        log.info(
                "marathonbet sync tick: fetched={}, eligible={}, matched={}, saved={}, sse={}, failures={}",
                tournamentFetched, eligible, matched, saved, sseCalls, mappingFailures
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
        JsonNode tournamentRoot = tournamentClient.fetchTournament(tournamentId);
        List<MarathonbetPrematchEvent> prematch = MarathonbetTournamentParser.parsePrematchEvents(tournamentRoot);

        MarathonbetSyncRun run = MarathonbetSyncRun.builder()
                .startedAt(LocalDateTime.now())
                .manual(true)
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
                true
        );

        run.setMatchesEligible(counters.matchesEligible());
        run.setMatchesMatched(counters.matchesMatched());
        run.setMergedSaved(counters.mergedSaved());
        run.setSseCalls(counters.sseCalls());
        run.setMappingFailures(counters.mappingFailures());
        run.setFailedGameResultIds(counters.failedGameResultIds());
        run.setFinishedAt(LocalDateTime.now());
        syncRunRepository.save(run);

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

    private SlotSyncCounters syncLeagueMatchday(
            League league,
            int matchday,
            String season,
            List<MarathonbetPrematchEvent> prematch,
            List<String> gameResultIds,
            boolean failWhenNoPending
    ) {
        String leagueCode = league.getLeagueCode().name();
        List<GameResultRecord> matches = footballDataSyncService.getMatches(
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

        LocalDateTime now = LocalDateTime.now();
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
                Thread.sleep(properties.getSseDelayMs());
                JsonNode eventRoot = scrapeService.fetchEventSnapshot(event.getTreeId());
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
        Season active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE)
                .orElseThrow(() -> new BadRequestException("seasonDatesRequired"));
        return matchdaySupport.resolveFootballDataSeasonYear(active, league.getLeagueCode());
    }

    private void applyJitter() {
        int minutes = properties.getSyncJitterMinutes();
        if (minutes <= 0) {
            return;
        }
        int delayMs = ThreadLocalRandom.current().nextInt(-minutes * 60_000, minutes * 60_000 + 1);
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
