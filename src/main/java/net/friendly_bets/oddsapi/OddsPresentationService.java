package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsEventMarketsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.odds.GameResultOdds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.OddsApiClient;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.repositories.GameResultOddsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.GetEntityService;
import net.friendly_bets.services.TeamAliasResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OddsPresentationService {

    private final GameResultRecordRepository gameResultRecordRepository;
    private final GameResultOddsRepository gameResultOddsRepository;
    private final OddsApiClient oddsApiClient;
    private final OddsApiProperties properties;
    private final OddsApiEventMatcher eventMatcher;
    private final GetEntityService getEntityService;

    public OddsEventMarketsDto getMarketsForGameResult(String gameResultId) {
        if (!oddsApiClient.isConfigured()) {
            throw new BadRequestException("oddsApiNotConfigured");
        }
        GameResultRecord match = gameResultRecordRepository.findById(gameResultId)
                .orElseThrow(() -> new NotFoundException("GameResult", gameResultId));
        LocalDateTime now = LocalDateTime.now();
        if (!GameResultNotStarted.isNotStarted(match, now)) {
            throw new BadRequestException("matchAlreadyStarted");
        }

        List<String> bookmakers = properties.getBookmakers();
        if (bookmakers == null || bookmakers.isEmpty()) {
            throw new BadRequestException("oddsApiBookmakersNotConfigured");
        }

        Map<String, String> canonicalByLower = OddsBookmakerKeys.mapApiKeysToConfigured(bookmakers);
        List<GameResultOdds> cached = gameResultOddsRepository.findByGameResultId(gameResultId);
        boolean stale = isStale(cached, now);

        Map<String, List<OddsApiMarketDto>> bookmakerMarkets;
        LocalDateTime fetchedAt;
        if (stale) {
            bookmakerMarkets = refreshFromApi(match, bookmakers, now);
            fetchedAt = now;
        } else {
            bookmakerMarkets = fromCached(cached);
            fetchedAt = cached.stream()
                    .map(GameResultOdds::getFetchedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(now);
        }

        if (bookmakerMarkets.isEmpty()) {
            throw new BadRequestException("oddsNotAvailable");
        }

        OddsMatchContext matchContext = buildMatchContext(match);
        List<OddsMarketGroup> groups = OddsGroupBuilder.build(bookmakerMarkets, canonicalByLower, matchContext);
        OddsSelectionKey.enrichGroups(groups);
        enrichBetTitles(groups);

        return OddsEventMarketsDto.builder()
                .gameResultId(gameResultId)
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .status(match.getStatus())
                .kickoffUtc(match.getUtcDate())
                .bookmakers(new ArrayList<>(canonicalByLower.values()))
                .marketGroups(groups)
                .fetchedAt(fetchedAt)
                .build();
    }

    public Map<String, List<OddsApiMarketDto>> refreshFromApi(
            GameResultRecord match,
            List<String> bookmakers,
            LocalDateTime fetchedAt
    ) {
        Long eventId = resolveEventId(match);
        List<OddsApiEventOddsDto> responses = oddsApiClient.fetchOddsMulti(List.of(eventId), bookmakers);
        if (responses == null || responses.isEmpty()) {
            return Map.of();
        }
        OddsApiEventOddsDto eventOdds = responses.get(0);
        Map<String, List<OddsApiMarketDto>> byBookmaker = eventOdds.getBookmakers() != null
                ? eventOdds.getBookmakers()
                : Map.of();

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
            entity.setOddsApiEventId(eventId);
            entity.setFetchedAt(fetchedAt);
            entity.setMarkets(OddsApiMarketMapper.toMarkets(markets));
            gameResultOddsRepository.save(entity);
        }
        return byBookmaker;
    }

    public Optional<OddsLineSelection> findSelection(
            String gameResultId,
            String selectionKey,
            String bookmaker
    ) {
        OddsEventMarketsDto markets = getMarketsForGameResult(gameResultId);
        for (OddsMarketGroup group : markets.getMarketGroups()) {
            if (group.getRows() == null) {
                continue;
            }
            for (var row : group.getRows()) {
                if (selectionKey.equals(row.getSelectionKey())) {
                    String odds = row.getBookmakerOdds() != null
                            ? row.getBookmakerOdds().get(bookmaker)
                            : null;
                    if (odds == null || odds.isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(new OddsLineSelection(group.getCategory(), row, odds));
                }
            }
        }
        return Optional.empty();
    }

    public record OddsLineSelection(String category, net.friendly_bets.models.odds.OddsLineRow row, String odds) {
    }

    private Long resolveEventId(GameResultRecord match) {
        if (match.getOddsApiEventId() != null && match.getOddsApiEventId() > 0) {
            return match.getOddsApiEventId();
        }
        String leagueSlug = OddsApiLeagueMapping.toLeagueSlug(
                net.friendly_bets.models.League.LeagueCode.valueOf(match.getLeagueCode()),
                properties
        ).orElseThrow(() -> new BadRequestException("oddsLeagueSlugNotConfigured"));

        List<OddsApiEventDto> events = oddsApiClient.fetchEvents(leagueSlug, "pending");
        Optional<Long> resolved = eventMatcher.resolveAndPersistEventId(
                match,
                events,
                match.getLeagueCode(),
                match.getSeason(),
                match.getMatchday()
        );
        return resolved.orElseThrow(() -> new BadRequestException("oddsEventNotMapped"));
    }

    private boolean isStale(List<GameResultOdds> cached, LocalDateTime now) {
        if (cached == null || cached.isEmpty()) {
            return true;
        }
        int minutes = properties.getPresentationStaleMinutes();
        LocalDateTime threshold = now.minusMinutes(Math.max(1, minutes));
        return cached.stream()
                .anyMatch(o -> o.getFetchedAt() == null || o.getFetchedAt().isBefore(threshold));
    }

    private Map<String, List<OddsApiMarketDto>> fromCached(List<GameResultOdds> cached) {
        Map<String, List<OddsApiMarketDto>> result = new LinkedHashMap<>();
        for (GameResultOdds doc : cached) {
            if (doc.getMarkets() == null || doc.getMarkets().isEmpty()) {
                continue;
            }
            result.put(doc.getBookmaker(), OddsStoredMarketsConverter.toApiMarkets(doc.getMarkets()));
        }
        return result;
    }

    private OddsMatchContext buildMatchContext(GameResultRecord match) {
        String home = getEntityService.getTeamOrThrow(match.getHomeTeamId()).getTitle();
        String away = getEntityService.getTeamOrThrow(match.getAwayTeamId()).getTitle();
        return OddsMatchContext.of(home, away);
    }

    private void enrichBetTitles(List<OddsMarketGroup> groups) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            if (group.getRows() == null || group.getCategory() == null) {
                continue;
            }
            for (var row : group.getRows()) {
                try {
                    row.setBetTitle(OddsSelectionBetTitleMapper.toBetTitle(group.getCategory(), row));
                } catch (BadRequestException ignored) {
                    row.setBetTitle(null);
                }
            }
        }
    }
}
