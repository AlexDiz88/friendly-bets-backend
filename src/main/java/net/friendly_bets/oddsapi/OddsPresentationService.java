package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsEventMarketsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.marathonbet.MarathonbetBookmaker;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.odds.GameResultMergedOdds;
import net.friendly_bets.models.odds.GameResultOdds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.OddsApiClient;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiEventOddsDto;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;
import net.friendly_bets.oddsapi.config.OddsApiProperties;
import net.friendly_bets.oddsapi.mapping.BetTitleKey;
import net.friendly_bets.repositories.GameResultOddsRepository;
import net.friendly_bets.repositories.GameResultRecordRepository;
import net.friendly_bets.services.GetEntityService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private final OddsMergedOddsService oddsMergedOddsService;

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
        List<String> canonicalBookmakers = new ArrayList<>(canonicalByLower.values());

        Optional<GameResultMergedOdds> mergedSnapshot = oddsMergedOddsService.findByGameResultId(gameResultId);
        if (mergedSnapshot.isPresent() && mergedSnapshot.get().getFrozenAt() != null) {
            List<OddsMarketGroup> frozenGroups = new ArrayList<>(mergedSnapshot.get().getMarketGroups());
            List<String> presentationBookmakers = resolvePresentationBookmakers(mergedSnapshot, canonicalBookmakers);
            prepareMarketGroupsForPresentation(frozenGroups, presentationBookmakers);
            return toDto(match, frozenGroups, mergedSnapshot.get().getFetchedAt(), presentationBookmakers);
        }

        List<OddsMarketGroup> groups;
        LocalDateTime fetchedAt;

        if (isMergedSnapshotUsable(mergedSnapshot, now)) {
            groups = new ArrayList<>(mergedSnapshot.get().getMarketGroups());
            fetchedAt = mergedSnapshot.get().getFetchedAt() != null ? mergedSnapshot.get().getFetchedAt() : now;
        } else if (mergedSnapshotHasMarathonbet(mergedSnapshot)) {
            // Синк Marathonbet — источник истины; не перезаписывать odds-api при открытии диалога.
            groups = new ArrayList<>(mergedSnapshot.get().getMarketGroups());
            fetchedAt = mergedSnapshot.get().getFetchedAt() != null ? mergedSnapshot.get().getFetchedAt() : now;
        } else {
            List<GameResultOdds> cached = gameResultOddsRepository.findByGameResultId(gameResultId);
            boolean oddsApiCacheStale = isStale(cached, now);
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets = oddsApiCacheStale
                    ? refreshFromApi(match, bookmakers, now)
                    : fromCached(cached);
            fetchedAt = oddsApiCacheStale ? now : cached.stream()
                    .map(GameResultOdds::getFetchedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(now);

            if (bookmakerMarkets.isEmpty()) {
                if (mergedSnapshot.isPresent() && mergedSnapshot.get().getMarketGroups() != null
                        && !mergedSnapshot.get().getMarketGroups().isEmpty()) {
                    groups = mergedSnapshot.get().getMarketGroups();
                    fetchedAt = mergedSnapshot.get().getFetchedAt() != null ? mergedSnapshot.get().getFetchedAt() : now;
                } else {
                    throw new BadRequestException("oddsNotAvailable");
                }
            } else {
                OddsMatchContext matchContext = buildMatchContext(match);
                var mergeResult = oddsMergedOddsService.buildAndPersist(
                        match,
                        bookmakerMarkets,
                        canonicalByLower,
                        matchContext,
                        canonicalBookmakers,
                        fetchedAt,
                        false
                );
                groups = mergeResult.getMarketGroups();
            }
        }

        List<OddsMarketGroup> presentationGroups = new ArrayList<>(groups);
        List<String> presentationBookmakers = resolvePresentationBookmakers(mergedSnapshot, canonicalBookmakers);
        prepareMarketGroupsForPresentation(presentationGroups, presentationBookmakers);
        presentationGroups = presentationGroups.stream()
                .filter(g -> (g.getRows() != null && !g.getRows().isEmpty())
                        || (g.getSubgroups() != null && !g.getSubgroups().isEmpty()))
                .toList();

        if (presentationGroups.isEmpty()) {
            throw new BadRequestException("oddsNotAvailable");
        }

        return toDto(match, presentationGroups, fetchedAt, presentationBookmakers);
    }

    private List<String> resolvePresentationBookmakers(
            Optional<GameResultMergedOdds> mergedSnapshot,
            List<String> oddsApiBookmakers
    ) {
        if (mergedSnapshot.isPresent()) {
            List<String> fromMerged = mergedSnapshot.get().getBookmakers();
            if (fromMerged != null && !fromMerged.isEmpty()) {
                return new ArrayList<>(fromMerged);
            }
        }
        return oddsApiBookmakers;
    }

    private void prepareMarketGroupsForPresentation(List<OddsMarketGroup> groups, List<String> bookmakers) {
        filterToPresentationBookmakers(groups, bookmakers);
        oddsMergedOddsService.enrichBetTitles(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
        OddsSelectionKey.enrichGroups(groups);
        OddsResultTotalEnricher.appendCalculatedGroups(groups, bookmakers);
        OddsHalfCorrectScoreSubgroupSplitter.splitIntoSubgroups(groups);
        OddsResultTotalEnricher.applyCategoryMetadata(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
    }

    private static void filterToPresentationBookmakers(List<OddsMarketGroup> groups, List<String> bookmakers) {
        if (groups == null || bookmakers == null || bookmakers.isEmpty()) {
            return;
        }
        Set<String> allowed = new LinkedHashSet<>(bookmakers);
        filterGroupsRecursive(groups, allowed);
    }

    private static void filterGroupsRecursive(List<OddsMarketGroup> groups, Set<String> allowed) {
        for (OddsMarketGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (group.getRows() != null) {
                for (var row : group.getRows()) {
                    if (row.getBookmakerOdds() != null) {
                        row.getBookmakerOdds().entrySet().removeIf(e -> !allowed.contains(e.getKey()));
                    }
                    if (row.getBookmakerSourcePaths() != null) {
                        row.getBookmakerSourcePaths().entrySet().removeIf(e -> !allowed.contains(e.getKey()));
                    }
                    row.setBestOdds(null);
                    row.setBestBookmaker(null);
                    OddsSelectionKey.applyBestOdds(row);
                }
            }
            if (group.getSubgroups() != null && !group.getSubgroups().isEmpty()) {
                filterGroupsRecursive(group.getSubgroups(), allowed);
            }
        }
    }

    private static boolean mergedSnapshotHasMarathonbet(Optional<GameResultMergedOdds> mergedSnapshot) {
        if (mergedSnapshot.isEmpty()) {
            return false;
        }
        GameResultMergedOdds merged = mergedSnapshot.get();
        if (merged.getMarketGroups() == null || merged.getMarketGroups().isEmpty()) {
            return false;
        }
        return merged.getBookmakers() != null
                && merged.getBookmakers().stream().anyMatch(MarathonbetBookmaker.KEY::equalsIgnoreCase);
    }

    public Map<String, List<OddsApiMarketDto>> refreshFromApi(
            GameResultRecord match,
            List<String> bookmakers,
            LocalDateTime fetchedAt
    ) {
        if (!GameResultNotStarted.isNotStarted(match, fetchedAt)) {
            return Map.of();
        }
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
            Optional<OddsLineSelection> found = findSelectionInGroup(group, selectionKey, bookmaker);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<OddsLineSelection> findSelectionInGroup(
            OddsMarketGroup group,
            String selectionKey,
            String bookmaker
    ) {
        if (group.getRows() != null) {
            for (var row : group.getRows()) {
                if (selectionKey.equals(row.getSelectionKey())) {
                    String odds = row.getBookmakerOdds() != null
                            ? row.getBookmakerOdds().get(bookmaker)
                            : null;
                    if (odds == null || odds.isBlank()) {
                        odds = row.getBestOdds();
                    }
                    if (odds == null || odds.isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(new OddsLineSelection(group.getCategory(), row, odds));
                }
            }
        }
        if (group.getSubgroups() != null) {
            for (OddsMarketGroup sub : group.getSubgroups()) {
                Optional<OddsLineSelection> found = findSelectionInGroup(sub, selectionKey, bookmaker);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    public Optional<OddsLineSelection> findByBetTitle(
            String gameResultId,
            short betTitleCode,
            boolean isNot,
            String bookmaker
    ) {
        BetTitleKey key = new BetTitleKey(betTitleCode, isNot);
        OddsEventMarketsDto markets = getMarketsForGameResult(gameResultId);
        for (OddsMarketGroup group : markets.getMarketGroups()) {
            Optional<OddsLineSelection> found = findByBetTitleInGroup(group, key, bookmaker);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<OddsLineSelection> findByBetTitleInGroup(
            OddsMarketGroup group,
            BetTitleKey key,
            String bookmaker
    ) {
        if (group.getRows() != null) {
            for (var row : group.getRows()) {
                BetTitleKey rowKey = BetTitleKey.from(row.getBetTitle());
                if (key.equals(rowKey)) {
                    String odds = row.getBookmakerOdds() != null
                            ? row.getBookmakerOdds().get(bookmaker)
                            : null;
                    if (odds == null || odds.isBlank()) {
                        odds = row.getBestOdds();
                    }
                    if (odds == null || odds.isBlank()) {
                        return Optional.empty();
                    }
                    return Optional.of(new OddsLineSelection(group.getCategory(), row, odds));
                }
            }
        }
        if (group.getSubgroups() != null) {
            for (OddsMarketGroup sub : group.getSubgroups()) {
                Optional<OddsLineSelection> found = findByBetTitleInGroup(sub, key, bookmaker);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    public record OddsLineSelection(String category, net.friendly_bets.models.odds.OddsLineRow row, String odds) {
    }

    private OddsEventMarketsDto toDto(
            GameResultRecord match,
            List<OddsMarketGroup> groups,
            LocalDateTime fetchedAt,
            List<String> bookmakers
    ) {
        return OddsEventMarketsDto.builder()
                .gameResultId(match.getId())
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .status(match.getStatus())
                .kickoffUtc(match.getUtcDate())
                .bookmakers(bookmakers)
                .marketGroups(groups)
                .fetchedAt(fetchedAt)
                .build();
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

    private boolean isMergedSnapshotUsable(Optional<GameResultMergedOdds> mergedSnapshot, LocalDateTime now) {
        if (mergedSnapshot.isEmpty()) {
            return false;
        }
        GameResultMergedOdds merged = mergedSnapshot.get();
        if (merged.getMarketGroups() == null || merged.getMarketGroups().isEmpty()) {
            return false;
        }
        return !isMergedStale(merged, now);
    }

    private boolean isMergedStale(GameResultMergedOdds merged, LocalDateTime now) {
        if (merged.getFetchedAt() == null) {
            return true;
        }
        int minutes = properties.getPresentationStaleMinutes();
        LocalDateTime threshold = now.minusMinutes(Math.max(1, minutes));
        return merged.getFetchedAt().isBefore(threshold);
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
}
