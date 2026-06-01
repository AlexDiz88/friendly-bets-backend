package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsDemoDebugDto;
import net.friendly_bets.dto.OddsDemoEventDetailDto;
import net.friendly_bets.dto.OddsDemoEventSummaryDto;
import net.friendly_bets.dto.OddsDemoRefreshResultDto;
import net.friendly_bets.dto.OddsMappingTraceEntryDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.footballdata.ApiSyncIssueService;
import net.friendly_bets.models.odds.OddsDemoSnapshot;
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
        if (!oddsApiClient.isConfigured()) {
            throw new BadRequestException("oddsApiKeyNotConfigured");
        }
        if (leagueSlug == null || leagueSlug.isBlank()) {
            throw new BadRequestException("leagueSlugRequired");
        }

        int maxEvents = limit != null && limit > 0 ? Math.min(limit, 50) : DEFAULT_DEMO_LIMIT;
        List<String> bookmakers = properties.getBookmakers();
        if (bookmakers == null || bookmakers.isEmpty()) {
            throw new BadRequestException("oddsApiBookmakersNotConfigured");
        }

        Map<String, String> canonicalByLower = OddsBookmakerKeys.mapApiKeysToConfigured(bookmakers);
        List<OddsApiEventDto> events = oddsApiClient.fetchEvents(leagueSlug.trim(), "pending");
        events = events.stream().filter(this::isDemoEventEligible).toList();
        if (events.isEmpty()) {
            return OddsDemoRefreshResultDto.builder()
                    .leagueSlug(leagueSlug)
                    .eventsStored(0)
                    .build();
        }

        oddsDemoSnapshotRepository.deleteByLeagueSlug(leagueSlug.trim());

        List<OddsApiEventDto> slice = events.size() <= maxEvents ? events : events.subList(0, maxEvents);
        LocalDateTime fetchedAt = LocalDateTime.now();
        int stored = 0;

        for (int i = 0; i < slice.size(); i += 10) {
            List<OddsApiEventDto> batchEvents = slice.subList(i, Math.min(i + 10, slice.size()));
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
                OddsApiEventOddsDto oddsDto = oddsById.get(event.getId());
                OddsMatchContext matchContext = OddsMatchContext.of(event.getHome(), event.getAway());
                Map<String, List<OddsApiMarketDto>> rawByBookmaker = oddsDto != null && oddsDto.getBookmakers() != null
                        ? oddsDto.getBookmakers()
                        : Map.of();
                OddsMergeResult mergeResult = oddsMappingPipeline.build(rawByBookmaker, canonicalByLower, matchContext);
                OddsSelectionKey.enrichGroups(mergeResult.getMarketGroups());

                oddsDemoSnapshotRepository.save(OddsDemoSnapshot.builder()
                        .oddsApiEventId(event.getId())
                        .home(event.getHome())
                        .away(event.getAway())
                        .eventDate(event.getDate())
                        .leagueSlug(leagueSlug.trim())
                        .status(event.getStatus())
                        .bookmakers(new ArrayList<>(canonicalByLower.values()))
                        .marketGroups(mergeResult.getMarketGroups())
                        .rawBookmakers(copyRawBookmakers(rawByBookmaker, canonicalByLower))
                        .fetchedAt(fetchedAt)
                        .build());
                stored++;
            }
        }

        log.info("odds demo refresh: league={} stored={} bookmakers={}", leagueSlug, stored, bookmakers);
        return OddsDemoRefreshResultDto.builder()
                .leagueSlug(leagueSlug)
                .eventsStored(stored)
                .bookmakers(bookmakers)
                .build();
    }

    public List<OddsDemoEventSummaryDto> listByLeague(String leagueSlug) {
        return oddsDemoSnapshotRepository.findByLeagueSlugOrderByEventDateAsc(leagueSlug).stream()
                .map(OddsDemoEventSummaryDto::from)
                .collect(Collectors.toList());
    }

    public OddsDemoEventDetailDto getByEventId(long eventId) {
        OddsDemoSnapshot snapshot = oddsDemoSnapshotRepository.findByOddsApiEventId(eventId)
                .orElseThrow(() -> new NotFoundException("oddsDemoEvent", String.valueOf(eventId)));
        return OddsDemoEventDetailDto.from(snapshot);
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
        Map<String, List<OddsApiMarketDto>> raw = snapshot.getRawBookmakers() != null
                ? snapshot.getRawBookmakers()
                : Map.of();

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

        OddsMergeResult mergeResult = oddsMappingPipeline.build(raw, canonicalByLower, matchContext);

        return OddsDemoDebugDto.builder()
                .oddsApiEventId(snapshot.getOddsApiEventId())
                .home(snapshot.getHome())
                .away(snapshot.getAway())
                .bookmakers(snapshot.getBookmakers())
                .fetchedAt(snapshot.getFetchedAt())
                .rawBookmakers(toRawBookmakersView(raw))
                .mappingTraceByBookmaker(traceByBookmaker)
                .mergedMarketGroups(mergeResult.getMarketGroups())
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

    private Map<String, List<OddsApiMarketDto>> copyRawBookmakers(
            Map<String, List<OddsApiMarketDto>> fromApi,
            Map<String, String> canonicalByLower
    ) {
        Map<String, List<OddsApiMarketDto>> result = new LinkedHashMap<>();
        if (fromApi == null) {
            return result;
        }
        for (Map.Entry<String, List<OddsApiMarketDto>> entry : fromApi.entrySet()) {
            String canonical = OddsBookmakerKeys.resolveCanonical(entry.getKey(), canonicalByLower);
            if (canonical != null && entry.getValue() != null) {
                result.put(canonical, entry.getValue());
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

    private void recordUnmappedTeamHints(OddsApiEventDto event) {
        apiSyncIssueService.recordUnmappedOddsApiTeamNameHint(
                event.getHome(), event.getHomeId(), true, null);
        apiSyncIssueService.recordUnmappedOddsApiTeamNameHint(
                event.getAway(), event.getAwayId(), false, null);
    }
}
