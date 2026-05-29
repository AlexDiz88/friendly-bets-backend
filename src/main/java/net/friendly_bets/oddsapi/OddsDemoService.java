package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsDemoEventDetailDto;
import net.friendly_bets.dto.OddsDemoEventSummaryDto;
import net.friendly_bets.dto.OddsDemoRefreshResultDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.odds.OddsDemoSnapshot;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.OddsApiClient;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.repositories.OddsDemoSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
                OddsApiEventOddsDto oddsDto = oddsById.get(event.getId());
                OddsMatchContext matchContext = OddsMatchContext.of(event.getHome(), event.getAway());
                List<OddsMarketGroup> marketGroups = oddsDto != null
                        ? OddsGroupBuilder.build(oddsDto.getBookmakers(), canonicalByLower, matchContext)
                        : List.of();

                oddsDemoSnapshotRepository.save(OddsDemoSnapshot.builder()
                        .oddsApiEventId(event.getId())
                        .home(event.getHome())
                        .away(event.getAway())
                        .eventDate(event.getDate())
                        .leagueSlug(leagueSlug.trim())
                        .status(event.getStatus())
                        .bookmakers(new ArrayList<>(canonicalByLower.values()))
                        .marketGroups(marketGroups)
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
}
