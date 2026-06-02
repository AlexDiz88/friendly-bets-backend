package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsDemoClearResultDto;
import net.friendly_bets.dto.OddsDemoDebugDto;
import net.friendly_bets.dto.OddsDemoEventDetailDto;
import net.friendly_bets.dto.OddsDemoEventIdDto;
import net.friendly_bets.dto.OddsDemoEventSummaryDto;
import net.friendly_bets.dto.OddsDemoRefreshResultDto;
import net.friendly_bets.dto.OddsMappingTraceEntryDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.models.odds.OddsDemoSnapshot;
import net.friendly_bets.models.odds.OddsMarket;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.OddsApiClient;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsBookmakerAdapterRegistry;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.oddsapi.mapping.OddsMappingPipeline;
import net.friendly_bets.oddsapi.poisson.OddsResultTotalEnricher;
import net.friendly_bets.repositories.OddsDemoSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OddsDemoService {

    private static final Logger log = LoggerFactory.getLogger(OddsDemoService.class);
    private static final int DEFAULT_DEMO_LIMIT = 20;

    private final OddsApiClient oddsApiClient;
    private final OddsApiProperties properties;
    private final OddsDemoSnapshotRepository oddsDemoSnapshotRepository;
    private final ApiSyncIssueService apiSyncIssueService;
    private final OddsMappingPipeline oddsMappingPipeline;
    private final OddsBookmakerAdapterRegistry adapterRegistry;

    public OddsDemoRefreshResultDto refreshLeagueDemo(String leagueSlug, Integer limit) {
        List<String> bookmakers = requireBookmakers();
        String slug = requireLeagueSlug(leagueSlug);

        int maxEvents = limit != null && limit > 0 ? Math.min(limit, 50) : DEFAULT_DEMO_LIMIT;
        List<OddsApiEventDto> events = fetchEligibleEvents(slug);
        if (events.isEmpty()) {
            return OddsDemoRefreshResultDto.builder()
                    .leagueSlug(slug)
                    .eventsStored(0)
                    .build();
        }

        oddsDemoSnapshotRepository.deleteByLeagueSlug(slug);

        List<OddsApiEventDto> slice = events.size() <= maxEvents ? events : events.subList(0, maxEvents);
        int stored = storeEventsBatch(slug, slice, bookmakers);
        log.info("odds demo refresh: league={} stored={} bookmakers={}", slug, stored, bookmakers);
        return OddsDemoRefreshResultDto.builder()
                .leagueSlug(slug)
                .eventsStored(stored)
                .bookmakers(bookmakers)
                .build();
    }

    public OddsDemoRefreshResultDto refreshEventsByIds(String leagueSlug, List<Long> eventIds) {
        List<String> bookmakers = requireBookmakers();
        String slug = requireLeagueSlug(leagueSlug);
        if (eventIds == null || eventIds.isEmpty()) {
            throw new BadRequestException("oddsDemoEventIdsRequired");
        }

        List<Long> uniqueIds = eventIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .limit(50)
                .toList();
        if (uniqueIds.isEmpty()) {
            throw new BadRequestException("oddsDemoEventIdsRequired");
        }

        Map<Long, OddsApiEventDto> eventMeta = fetchEligibleEvents(slug).stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(OddsApiEventDto::getId, e -> e, (a, b) -> a, LinkedHashMap::new));

        int stored = 0;
        for (int i = 0; i < uniqueIds.size(); i += 10) {
            List<Long> batchIds = uniqueIds.subList(i, Math.min(i + 10, uniqueIds.size()));
            List<OddsApiEventOddsDto> oddsList = oddsApiClient.fetchOddsMulti(batchIds, bookmakers);
            Map<Long, OddsApiEventOddsDto> oddsById = new LinkedHashMap<>();
            for (OddsApiEventOddsDto odds : oddsList) {
                if (odds != null && odds.getId() != null) {
                    oddsById.put(odds.getId(), odds);
                }
            }

            for (Long eventId : batchIds) {
                OddsApiEventDto meta = eventMeta.get(eventId);
                OddsApiEventOddsDto oddsDto = oddsById.get(eventId);
                if (meta != null) {
                    recordUnmappedTeamHints(meta);
                }
                if (storeEventSnapshot(slug, eventId, meta, oddsDto, bookmakers)) {
                    stored++;
                }
            }
        }

        log.info("odds demo refresh by ids: league={} requested={} stored={}", slug, uniqueIds.size(), stored);
        return OddsDemoRefreshResultDto.builder()
                .leagueSlug(slug)
                .eventsStored(stored)
                .bookmakers(bookmakers)
                .build();
    }

    public List<OddsDemoEventIdDto> listEventIdsFromApi(String leagueSlug, Integer limit) {
        String slug = requireLeagueSlug(leagueSlug);
        int maxEvents = limit != null && limit > 0 ? Math.min(limit, 100) : 50;
        return fetchEligibleEvents(slug).stream()
                .limit(maxEvents)
                .map(OddsDemoEventIdDto::from)
                .toList();
    }

    public void deleteEvent(long eventId) {
        if (oddsDemoSnapshotRepository.findByOddsApiEventId(eventId).isEmpty()) {
            throw new NotFoundException("oddsDemoEvent", String.valueOf(eventId));
        }
        oddsDemoSnapshotRepository.deleteByOddsApiEventId(eventId);
    }

    public OddsDemoClearResultDto clearLeague(String leagueSlug) {
        String slug = requireLeagueSlug(leagueSlug);
        long count = oddsDemoSnapshotRepository.countByLeagueSlug(slug);
        oddsDemoSnapshotRepository.deleteByLeagueSlug(slug);
        return OddsDemoClearResultDto.builder().deletedCount((int) count).build();
    }

    public OddsDemoClearResultDto clearAll() {
        long count = oddsDemoSnapshotRepository.count();
        oddsDemoSnapshotRepository.deleteAll();
        return OddsDemoClearResultDto.builder().deletedCount((int) count).build();
    }

    public List<OddsDemoEventSummaryDto> listByLeague(String leagueSlug) {
        return oddsDemoSnapshotRepository.findByLeagueSlugOrderByEventDateAsc(leagueSlug).stream()
                .map(OddsDemoEventSummaryDto::from)
                .collect(Collectors.toList());
    }

    public OddsDemoEventDetailDto getByEventId(long eventId) {
        OddsDemoSnapshot snapshot = oddsDemoSnapshotRepository.findByOddsApiEventId(eventId)
                .orElseThrow(() -> new NotFoundException("oddsDemoEvent", String.valueOf(eventId)));
        OddsDemoEventDetailDto dto = OddsDemoEventDetailDto.from(snapshot);
        if (dto.getMarketGroups() != null) {
            var groups = new ArrayList<>(dto.getMarketGroups());
            OddsSelectionKey.enrichGroups(groups);
            OddsResultTotalEnricher.appendCalculatedGroups(groups, snapshot.getBookmakers());
            OddsResultTotalEnricher.applyCategoryMetadata(groups);
            dto.setMarketGroups(groups);
        }
        return dto;
    }

    public OddsDemoDebugDto getDebugByEventId(long eventId) {
        OddsDemoSnapshot snapshot = oddsDemoSnapshotRepository.findByOddsApiEventId(eventId)
                .orElseThrow(() -> new NotFoundException("oddsDemoEvent", String.valueOf(eventId)));

        OddsMatchContext matchContext = OddsMatchContext.of(snapshot.getHome(), snapshot.getAway());
        Map<String, String> canonicalByLower = new LinkedHashMap<>();
        if (snapshot.getBookmakers() != null) {
            for (String bk : snapshot.getBookmakers()) {
                canonicalByLower.put(bk.toLowerCase(), bk);
            }
        }

        Map<String, List<OddsMappingTraceEntryDto>> traceByBookmaker = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        Map<String, List<OddsApiMarketDto>> raw = toApiRawBookmakers(snapshot.getRawBookmakers());

        for (String bookmaker : snapshot.getBookmakers() != null ? snapshot.getBookmakers() : List.<String>of()) {
            List<OddsApiMarketDto> markets = raw.get(bookmaker);
            List<MappedOddsQuote> quotes = adapterRegistry.mapBookmaker(bookmaker, markets, matchContext);
            List<OddsMappingTraceEntryDto> trace = quotes.stream()
                    .map(OddsDemoService::toTraceEntry)
                    .toList();
            traceByBookmaker.put(bookmaker, trace);
            for (MappedOddsQuote quote : quotes) {
                if (quote.getMappingStatus() != net.friendly_bets.oddsapi.mapping.OddsMappingStatus.OK) {
                    issues.add(bookmaker + ": " + quote.getRejectReason() + " " + quote.getRawRowJson());
                }
            }
        }

        OddsMergeResult mergeResult = oddsMappingPipeline.build(raw, canonicalByLower, matchContext, true);
        var mergedGroups = mergeResult.getMarketGroups();
        OddsSelectionKey.enrichGroups(mergedGroups);
        OddsResultTotalEnricher.appendCalculatedGroups(mergedGroups, snapshot.getBookmakers());
        OddsResultTotalEnricher.applyCategoryMetadata(mergedGroups);

        return OddsDemoDebugDto.builder()
                .oddsApiEventId(snapshot.getOddsApiEventId())
                .home(snapshot.getHome())
                .away(snapshot.getAway())
                .bookmakers(snapshot.getBookmakers())
                .fetchedAt(snapshot.getFetchedAt())
                .rawBookmakers(toRawBookmakersView(raw))
                .mappingTraceByBookmaker(traceByBookmaker)
                .mergedMarketGroups(mergedGroups)
                .mappingIssues(issues)
                .build();
    }

    private static OddsMappingTraceEntryDto toTraceEntry(MappedOddsQuote quote) {
        return OddsMappingTraceEntryDto.builder()
                .bookmaker(quote.getBookmaker())
                .marketName(quote.getMarketName())
                .rawRowJson(quote.getRawRowJson())
                .category(quote.getCategory() != null ? quote.getCategory().name() : null)
                .mappingStatus(quote.getMappingStatus().name())
                .rejectReason(quote.getRejectReason() != null ? quote.getRejectReason().name() : null)
                .rejectDetail(quote.getRejectDetail())
                .betTitleCode(quote.getBetTitle() != null ? quote.getBetTitle().getCode() : null)
                .betTitleIsNot(quote.getBetTitle() != null ? quote.getBetTitle().isNot() : null)
                .betTitleLabel(quote.getBetTitle() != null ? quote.getBetTitle().getLabel() : null)
                .odds(quote.getOdds())
                .selectionCode(quote.getSelectionCode())
                .line(quote.getLine())
                .build();
    }

    private static Map<String, List<OddsApiMarketDto>> toApiRawBookmakers(Map<String, List<OddsMarket>> stored) {
        Map<String, List<OddsApiMarketDto>> raw = new LinkedHashMap<>();
        if (stored == null) {
            return raw;
        }
        for (Map.Entry<String, List<OddsMarket>> entry : stored.entrySet()) {
            raw.put(entry.getKey(), OddsStoredMarketsConverter.toApiMarkets(entry.getValue()));
        }
        return raw;
    }

    private static Map<String, List<Object>> toRawBookmakersView(Map<String, List<OddsApiMarketDto>> raw) {
        Map<String, List<Object>> view = new LinkedHashMap<>();
        if (raw == null) {
            return view;
        }
        for (Map.Entry<String, List<OddsApiMarketDto>> entry : raw.entrySet()) {
            List<Object> markets = new ArrayList<>();
            if (entry.getValue() != null) {
                markets.addAll(entry.getValue());
            }
            view.put(entry.getKey(), markets);
        }
        return view;
    }

    private Map<String, List<OddsMarket>> copyRawBookmakers(
            Map<String, List<OddsApiMarketDto>> fromApi,
            Map<String, String> canonicalByLower
    ) {
        Map<String, List<OddsMarket>> result = new LinkedHashMap<>();
        if (fromApi == null) {
            return result;
        }
        for (Map.Entry<String, List<OddsApiMarketDto>> entry : fromApi.entrySet()) {
            String canonical = OddsBookmakerKeys.resolveCanonical(entry.getKey(), canonicalByLower);
            if (canonical != null && entry.getValue() != null) {
                result.put(canonical, OddsApiMarketMapper.toMarkets(entry.getValue()));
            }
        }
        return result;
    }

    private boolean isDemoEventEligible(OddsApiEventDto event) {
        if (event == null) {
            return false;
        }
        String status = event.getStatus();
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toLowerCase();
            if (!"pending".equals(normalized) && !"scheduled".equals(normalized)) {
                return false;
            }
        }
        if (event.getDate() == null || event.getDate().isBlank()) {
            return true;
        }
        try {
            OffsetDateTime kickoff = OffsetDateTime.parse(event.getDate());
            return kickoff.isAfter(OffsetDateTime.now());
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private List<String> requireBookmakers() {
        if (!oddsApiClient.isConfigured()) {
            throw new BadRequestException("oddsApiKeyNotConfigured");
        }
        List<String> bookmakers = properties.getBookmakers();
        if (bookmakers == null || bookmakers.isEmpty()) {
            throw new BadRequestException("oddsApiBookmakersNotConfigured");
        }
        return bookmakers;
    }

    private String requireLeagueSlug(String leagueSlug) {
        if (leagueSlug == null || leagueSlug.isBlank()) {
            throw new BadRequestException("leagueSlugRequired");
        }
        return leagueSlug.trim();
    }

    private List<OddsApiEventDto> fetchEligibleEvents(String leagueSlug) {
        requireBookmakers();
        return oddsApiClient.fetchEvents(leagueSlug, "pending").stream()
                .filter(this::isDemoEventEligible)
                .toList();
    }

    private int storeEventsBatch(String leagueSlug, List<OddsApiEventDto> events, List<String> bookmakers) {
        int stored = 0;
        for (int i = 0; i < events.size(); i += 10) {
            List<OddsApiEventDto> batchEvents = events.subList(i, Math.min(i + 10, events.size()));
            List<Long> eventIds = batchEvents.stream().map(OddsApiEventDto::getId).toList();
            List<OddsApiEventOddsDto> oddsList = oddsApiClient.fetchOddsMulti(eventIds, bookmakers);

            Map<Long, OddsApiEventOddsDto> oddsById = new LinkedHashMap<>();
            for (OddsApiEventOddsDto odds : oddsList) {
                if (odds != null && odds.getId() != null) {
                    oddsById.put(odds.getId(), odds);
                }
            }

            for (OddsApiEventDto event : batchEvents) {
                if (event == null || event.getId() == null) {
                    continue;
                }
                recordUnmappedTeamHints(event);
                if (storeEventSnapshot(leagueSlug, event.getId(), event, oddsById.get(event.getId()), bookmakers)) {
                    stored++;
                }
            }
        }
        return stored;
    }

    private boolean storeEventSnapshot(
            String leagueSlug,
            Long eventId,
            OddsApiEventDto eventMeta,
            OddsApiEventOddsDto oddsDto,
            List<String> bookmakers
    ) {
        if (eventId == null) {
            return false;
        }

        String home = eventMeta != null ? eventMeta.getHome() : oddsDto != null ? oddsDto.getHome() : null;
        String away = eventMeta != null ? eventMeta.getAway() : oddsDto != null ? oddsDto.getAway() : null;
        if (home == null || away == null) {
            return false;
        }

        Map<String, String> canonicalByLower = OddsBookmakerKeys.mapApiKeysToConfigured(bookmakers);
        OddsMatchContext matchContext = OddsMatchContext.of(home, away);
        Map<String, List<OddsApiMarketDto>> rawByBookmaker = oddsDto != null && oddsDto.getBookmakers() != null
                ? oddsDto.getBookmakers()
                : Map.of();
        OddsMergeResult mergeResult = oddsMappingPipeline.build(
                rawByBookmaker, canonicalByLower, matchContext, true);
        List<String> orderedBookmakers = orderBookmakersFromRaw(rawByBookmaker, canonicalByLower);
        var marketGroups = mergeResult.getMarketGroups();
        OddsSelectionKey.enrichGroups(marketGroups);
        OddsResultTotalEnricher.appendCalculatedGroups(marketGroups, orderedBookmakers);
        OddsResultTotalEnricher.applyCategoryMetadata(marketGroups);

        Optional<OddsDemoSnapshot> existing = oddsDemoSnapshotRepository.findIdByOddsApiEventId(eventId);
        OddsDemoSnapshot snapshot = existing.orElseGet(OddsDemoSnapshot::new);
        snapshot.setOddsApiEventId(eventId);
        snapshot.setHome(home);
        snapshot.setAway(away);
        snapshot.setEventDate(eventMeta != null ? eventMeta.getDate() : oddsDto != null ? oddsDto.getDate() : null);
        snapshot.setLeagueSlug(leagueSlug);
        snapshot.setStatus(eventMeta != null ? eventMeta.getStatus() : oddsDto != null ? oddsDto.getStatus() : null);
        snapshot.setBookmakers(orderedBookmakers);
        snapshot.setMarketGroups(marketGroups);
        snapshot.setRawBookmakers(copyRawBookmakers(rawByBookmaker, canonicalByLower));
        snapshot.setFetchedAt(LocalDateTime.now());
        oddsDemoSnapshotRepository.save(snapshot);
        return true;
    }

    private void recordUnmappedTeamHints(OddsApiEventDto event) {
        apiSyncIssueService.recordUnmappedOddsApiTeamNameHint(
                event.getHome(), event.getHomeId(), true, null);
        apiSyncIssueService.recordUnmappedOddsApiTeamNameHint(
                event.getAway(), event.getAwayId(), false, null);
    }

    /** Порядок колонок как в raw JSON odds-api (1xbet перед Bet365). */
    private static List<String> orderBookmakersFromRaw(
            Map<String, List<OddsApiMarketDto>> rawByBookmaker,
            Map<String, String> canonicalByLower
    ) {
        List<String> ordered = new ArrayList<>();
        if (rawByBookmaker != null) {
            for (String apiKey : rawByBookmaker.keySet()) {
                String canonical = OddsBookmakerKeys.resolveCanonical(apiKey, canonicalByLower);
                if (canonical != null && !ordered.contains(canonical)) {
                    ordered.add(canonical);
                }
            }
        }
        if (canonicalByLower != null) {
            for (String canonical : canonicalByLower.values()) {
                if (!ordered.contains(canonical)) {
                    ordered.add(canonical);
                }
            }
        }
        return ordered;
    }
}
