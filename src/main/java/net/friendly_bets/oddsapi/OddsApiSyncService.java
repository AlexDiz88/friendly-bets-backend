package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.footballdata.FootballDataCompetitionMapping;
import net.friendly_bets.footballdata.FootballDataCompetitionService;
import net.friendly_bets.footballdata.FootballDataMatchdaySupport;
import net.friendly_bets.models.League;
import net.friendly_bets.models.Season;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.odds.GameResultOdds;
import net.friendly_bets.oddsapi.client.OddsApiClient;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.repositories.GameResultOddsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.GetEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OddsApiSyncService {

    private static final Logger log = LoggerFactory.getLogger(OddsApiSyncService.class);

    private final OddsApiProperties properties;
    private final OddsApiClient oddsApiClient;
    private final SeasonsRepository seasonsRepository;
    private final FootballDataCompetitionService footballDataCompetitionService;
    private final FootballDataMatchdaySupport matchdaySupport;
    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultOddsRepository gameResultOddsRepository;
    private final OddsApiEventMatcher eventMatcher;
    private final GetEntityService getEntityService;

    /**
     * Принудительная синхронизация кэфов для тура лиги (админ / модератор).
     */
    public OddsApiSyncResult syncMatchday(String leagueId, int matchday, String season) {
        List<String> bookmakers = requireSyncReady();
        League league = getEntityService.getLeagueOrThrow(leagueId);
        if (league.getLeagueCode() == null) {
            throw new BadRequestException("leagueSlugRequired");
        }
        Optional<String> leagueSlug = OddsApiLeagueMapping.toLeagueSlug(league.getLeagueCode(), properties);
        if (leagueSlug.isEmpty()) {
            throw new BadRequestException("leagueSlugRequired");
        }

        String resolvedSeason = resolveStorageSeason(season, league);
        LocalDateTime now = LocalDateTime.now();
        LeagueMatchdaySyncCounters counters = syncLeagueMatchday(
                league,
                matchday,
                resolvedSeason,
                leagueSlug.get(),
                bookmakers,
                now,
                true
        );
        return counters.toResult();
    }

    public OddsApiSyncResult runTick() {
        if (!properties.isSyncEnabled() || !oddsApiClient.isConfigured()) {
            return OddsApiSyncResult.builder().build();
        }

        Optional<Season> active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
        if (active.isEmpty() || active.get().getLeagues() == null) {
            return OddsApiSyncResult.builder().build();
        }

        Season season = active.get();
        String externalSeasonYear = matchdaySupport.resolveFootballDataSeasonYear(season);
        LocalDateTime now = LocalDateTime.now();

        int leaguesProcessed = 0;
        int matchesEligible = 0;
        int oddsDocumentsSaved = 0;
        int matchesSkippedStarted = 0;
        int mappingFailures = 0;
        int teamMappingFailures = 0;

        List<String> bookmakers;
        try {
            bookmakers = requireSyncReady();
        } catch (BadRequestException e) {
            log.warn("odds-api sync tick skipped: {}", e.getMessage());
            return OddsApiSyncResult.builder().build();
        }

        for (League league : season.getLeagues()) {
            if (league == null || league.getLeagueCode() == null) {
                continue;
            }
            if (!FootballDataCompetitionMapping.isSupported(league.getLeagueCode())) {
                continue;
            }
            if (league.getTournamentFormatId() == null || league.getTournamentFormatId().isBlank()) {
                continue;
            }

            Optional<String> leagueSlug = OddsApiLeagueMapping.toLeagueSlug(league.getLeagueCode(), properties);
            if (leagueSlug.isEmpty()) {
                continue;
            }

            ExternalCompetitionInfoDto info = footballDataCompetitionService.getCompetitionInfoForLeague(
                    league.getId(),
                    externalSeasonYear
            );
            int matchday = info.getCurrentMatchday();

            LeagueMatchdaySyncCounters leagueResult = syncLeagueMatchday(
                    league,
                    matchday,
                    externalSeasonYear,
                    leagueSlug.get(),
                    bookmakers,
                    now,
                    false
            );
            if (leagueResult.leaguesProcessed() > 0) {
                leaguesProcessed += leagueResult.leaguesProcessed();
            }
            matchesEligible += leagueResult.matchesEligible();
            oddsDocumentsSaved += leagueResult.oddsDocumentsSaved();
            matchesSkippedStarted += leagueResult.matchesSkippedStarted();
            mappingFailures += leagueResult.mappingFailures();
            teamMappingFailures += leagueResult.teamMappingFailures();
        }

        log.info(
                "odds-api sync tick: leagues={}, eligible={}, saved={}, skippedStarted={}, mappingFailures={}, teamMappingFailures={}",
                leaguesProcessed,
                matchesEligible,
                oddsDocumentsSaved,
                matchesSkippedStarted,
                mappingFailures,
                teamMappingFailures
        );

        return OddsApiSyncResult.builder()
                .leaguesProcessed(leaguesProcessed)
                .matchesEligible(matchesEligible)
                .oddsDocumentsSaved(oddsDocumentsSaved)
                .matchesSkippedStarted(matchesSkippedStarted)
                .mappingFailures(mappingFailures)
                .teamMappingFailures(teamMappingFailures)
                .build();
    }

    private LeagueMatchdaySyncCounters syncLeagueMatchday(
            League league,
            int matchday,
            String season,
            String leagueSlug,
            List<String> bookmakers,
            LocalDateTime now,
            boolean failWhenNoPendingMatches
    ) {
        String leagueCode = league.getLeagueCode().name();
        List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                leagueCode,
                matchday,
                season
        );

        List<GameResultRecord> pending = new ArrayList<>();
        int matchesSkippedStarted = 0;
        for (GameResultRecord match : matches) {
            if (GameResultNotStarted.isNotStarted(match, now)) {
                pending.add(match);
            } else {
                matchesSkippedStarted++;
            }
        }

        if (pending.isEmpty()) {
            if (failWhenNoPendingMatches) {
                throw new BadRequestException("oddsSyncNoMatchdayMatches");
            }
            return new LeagueMatchdaySyncCounters(0, 0, 0, matchesSkippedStarted, 0, 0);
        }

        List<OddsApiEventDto> leagueEvents;
        try {
            leagueEvents = oddsApiClient.fetchEvents(leagueSlug, "pending");
        } catch (Exception e) {
            log.warn("odds-api events fetch failed for {}: {}", leagueSlug, e.getMessage());
            return new LeagueMatchdaySyncCounters(0, pending.size(), 0, matchesSkippedStarted, pending.size(), 0);
        }

        List<OddsApiEventDto> slotEvents = eventMatcher.eventsForPendingMatches(pending, leagueEvents);

        Map<Long, GameResultRecord> eventIdToMatch = new LinkedHashMap<>();
        int mappingFailures = 0;
        OddsTeamMappingCollector teamMappingCollector = new OddsTeamMappingCollector();
        for (GameResultRecord match : pending) {
            Optional<Long> eventId = eventMatcher.resolveAndPersistEventId(
                    match,
                    slotEvents,
                    leagueCode,
                    season,
                    matchday,
                    teamMappingCollector
            );
            if (eventId.isEmpty()) {
                mappingFailures++;
                continue;
            }
            eventIdToMatch.put(eventId.get(), match);
        }

        int oddsDocumentsSaved = 0;
        List<Long> eventIds = new ArrayList<>(eventIdToMatch.keySet());
        for (int i = 0; i < eventIds.size(); i += 10) {
            List<Long> batch = eventIds.subList(i, Math.min(i + 10, eventIds.size()));
            try {
                List<OddsApiEventOddsDto> oddsResponses = oddsApiClient.fetchOddsMulti(batch, bookmakers);
                oddsDocumentsSaved += persistOddsBatch(oddsResponses, eventIdToMatch, bookmakers, now);
            } catch (Exception e) {
                log.warn("odds-api multi odds failed for batch {}: {}", batch, e.getMessage());
                mappingFailures += batch.size();
            }
        }

        if (teamMappingCollector.getIssueCount() > 0) {
            OddsTeamMappingLog.logSyncSummary(
                    log,
                    teamMappingCollector.getIssueCount(),
                    leagueCode,
                    matchday,
                    season
            );
        }

        return new LeagueMatchdaySyncCounters(
                1,
                pending.size(),
                oddsDocumentsSaved,
                matchesSkippedStarted,
                mappingFailures,
                teamMappingCollector.getIssueCount()
        );
    }

    private List<String> requireSyncReady() {
        if (!properties.isSyncEnabled()) {
            throw new BadRequestException("oddsApiNotConfigured");
        }
        if (!oddsApiClient.isConfigured()) {
            throw new BadRequestException("oddsApiKeyNotConfigured");
        }
        List<String> bookmakers = properties.getBookmakers();
        if (bookmakers == null || bookmakers.isEmpty()) {
            throw new BadRequestException("oddsApiBookmakersNotConfigured");
        }
        return bookmakers;
    }

    private String resolveStorageSeason(String requestedSeason, League league) {
        if (requestedSeason != null && !requestedSeason.isBlank()) {
            return requestedSeason.trim();
        }
        Season active = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE)
                .orElseThrow(() -> new BadRequestException("seasonDatesRequired"));
        return matchdaySupport.resolveFootballDataSeasonYear(active, league.getLeagueCode());
    }

    private record LeagueMatchdaySyncCounters(
            int leaguesProcessed,
            int matchesEligible,
            int oddsDocumentsSaved,
            int matchesSkippedStarted,
            int mappingFailures,
            int teamMappingFailures
    ) {
        OddsApiSyncResult toResult() {
            return OddsApiSyncResult.builder()
                    .leaguesProcessed(leaguesProcessed)
                    .matchesEligible(matchesEligible)
                    .oddsDocumentsSaved(oddsDocumentsSaved)
                    .matchesSkippedStarted(matchesSkippedStarted)
                    .mappingFailures(mappingFailures)
                    .teamMappingFailures(teamMappingFailures)
                    .build();
        }
    }

    private int persistOddsBatch(
            List<OddsApiEventOddsDto> oddsResponses,
            Map<Long, GameResultRecord> eventIdToMatch,
            List<String> bookmakers,
            LocalDateTime fetchedAt
    ) {
        int saved = 0;
        if (oddsResponses == null) {
            return 0;
        }
        for (OddsApiEventOddsDto eventOdds : oddsResponses) {
            if (eventOdds == null || eventOdds.getId() == null) {
                continue;
            }
            GameResultRecord match = eventIdToMatch.get(eventOdds.getId());
            if (match == null) {
                continue;
            }
            Map<String, List<OddsApiMarketDto>> byBookmaker = eventOdds.getBookmakers();
            if (byBookmaker == null || byBookmaker.isEmpty()) {
                continue;
            }
            for (String bookmaker : bookmakers) {
                List<OddsApiMarketDto> markets = byBookmaker.get(bookmaker);
                if (markets == null || markets.isEmpty()) {
                    continue;
                }
                GameResultOdds entity = gameResultOddsRepository
                        .findByGameResultIdAndBookmaker(match.getId(), bookmaker)
                        .orElse(GameResultOdds.builder()
                                .gameResultId(match.getId())
                                .bookmaker(bookmaker)
                                .build());

                entity.setOddsApiEventId(eventOdds.getId());
                entity.setFetchedAt(fetchedAt);
                entity.setMarkets(OddsApiMarketMapper.toMarkets(markets));
                gameResultOddsRepository.save(entity);
                saved++;
            }
        }
        return saved;
    }
}
