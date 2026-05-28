package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ExternalCompetitionInfoDto;
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

        List<String> bookmakers = properties.getBookmakers();
        if (bookmakers == null || bookmakers.isEmpty()) {
            log.warn("odds-api sync: no bookmakers configured");
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
            String leagueCode = league.getLeagueCode().name();

            List<GameResultRecord> matches = gameResultRecordRepository.findByLeagueCodeAndMatchdayAndSeason(
                    leagueCode,
                    matchday,
                    externalSeasonYear
            );

            List<GameResultRecord> pending = new ArrayList<>();
            for (GameResultRecord match : matches) {
                if (GameResultNotStarted.isNotStarted(match, now)) {
                    pending.add(match);
                } else {
                    matchesSkippedStarted++;
                }
            }

            if (pending.isEmpty()) {
                continue;
            }

            leaguesProcessed++;
            matchesEligible += pending.size();

            List<OddsApiEventDto> leagueEvents;
            try {
                leagueEvents = oddsApiClient.fetchEvents(leagueSlug.get(), "pending");
            } catch (Exception e) {
                log.warn("odds-api events fetch failed for {}: {}", leagueSlug.get(), e.getMessage());
                mappingFailures += pending.size();
                continue;
            }

            Map<Long, GameResultRecord> eventIdToMatch = new LinkedHashMap<>();
            for (GameResultRecord match : pending) {
                Optional<Long> eventId = eventMatcher.resolveAndPersistEventId(
                        match,
                        leagueEvents,
                        leagueCode,
                        externalSeasonYear,
                        matchday
                );
                if (eventId.isEmpty()) {
                    mappingFailures++;
                    continue;
                }
                eventIdToMatch.put(eventId.get(), match);
            }

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
        }

        log.info(
                "odds-api sync tick: leagues={}, eligible={}, saved={}, skippedStarted={}, mappingFailures={}",
                leaguesProcessed,
                matchesEligible,
                oddsDocumentsSaved,
                matchesSkippedStarted,
                mappingFailures
        );

        return OddsApiSyncResult.builder()
                .leaguesProcessed(leaguesProcessed)
                .matchesEligible(matchesEligible)
                .oddsDocumentsSaved(oddsDocumentsSaved)
                .matchesSkippedStarted(matchesSkippedStarted)
                .mappingFailures(mappingFailures)
                .build();
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
