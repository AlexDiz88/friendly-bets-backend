package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.OddsEventMarketsDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.oddsapi.mapping.BetTitleKey;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.repositories.MatchScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only odds presentation from Mongo ({@code odds}).
 */
@Service
@RequiredArgsConstructor
public class OddsPresentationService {

    private static final List<String> DEFAULT_PRESENTATION_BOOKMAKERS = List.of("Marathonbet");

    private final MatchScheduleRepository matchScheduleRepository;
    private final OddsMergedOddsService oddsMergedOddsService;

    public OddsEventMarketsDto getMarketsForMatchSchedule(String matchScheduleId) {
        MatchSchedule match = matchScheduleRepository.findById(matchScheduleId)
                .orElseThrow(() -> new NotFoundException("MatchSchedule", matchScheduleId));
        Instant now = Instant.now();
        if (!MatchScheduleNotStarted.isNotStarted(match, now)) {
            throw new BadRequestException("matchAlreadyStarted");
        }

        Optional<Odds> mergedSnapshot = oddsMergedOddsService.findByMatchScheduleId(matchScheduleId);
        if (mergedSnapshot.isEmpty()
                || mergedSnapshot.get().getMarketGroups() == null
                || mergedSnapshot.get().getMarketGroups().isEmpty()) {
            throw new BadRequestException("oddsNotAvailable");
        }

        List<String> presentationBookmakers = resolvePresentationBookmakers(mergedSnapshot);

        List<OddsMarketGroup> presentationGroups = new ArrayList<>(mergedSnapshot.get().getMarketGroups());
        prepareMarketGroupsForPresentation(presentationGroups, presentationBookmakers);
        presentationGroups = presentationGroups.stream()
                .filter(g -> (g.getRows() != null && !g.getRows().isEmpty())
                        || (g.getSubgroups() != null && !g.getSubgroups().isEmpty()))
                .toList();

        if (presentationGroups.isEmpty()) {
            throw new BadRequestException("oddsNotAvailable");
        }

        Instant fetchedAt = mergedSnapshot.get().getFetchedAt() != null
                ? mergedSnapshot.get().getFetchedAt()
                : now;
        return toDto(match, presentationGroups, fetchedAt, presentationBookmakers);
    }

    private List<String> resolvePresentationBookmakers(Optional<Odds> mergedSnapshot) {
        if (mergedSnapshot.isPresent()) {
            List<String> fromMerged = mergedSnapshot.get().getBookmakers();
            if (fromMerged != null && !fromMerged.isEmpty()) {
                return new ArrayList<>(fromMerged);
            }
        }
        return new ArrayList<>(DEFAULT_PRESENTATION_BOOKMAKERS);
    }

    private void prepareMarketGroupsForPresentation(List<OddsMarketGroup> groups, List<String> bookmakers) {
        filterToPresentationBookmakers(groups, bookmakers);
        oddsMergedOddsService.enrichBetTitles(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
        OddsSelectionKey.enrichGroups(groups);
        OddsResultTotalEnricher.appendCalculatedGroups(groups, bookmakers);
        OddsHalfCorrectScoreSubgroupSplitter.splitIntoSubgroups(groups);
        OddsPeriodHandicapSubgroupSplitter.splitIntoSubgroups(groups);
        OddsResultTotalEnricher.applyCategoryMetadata(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
        OddsMerger.sortMarketGroupRows(groups);
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

    public Optional<OddsLineSelection> findSelection(
            String matchScheduleId,
            String selectionKey,
            String bookmaker
    ) {
        OddsEventMarketsDto markets = getMarketsForMatchSchedule(matchScheduleId);
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
            String matchScheduleId,
            short betTitleCode,
            boolean isNot,
            String bookmaker
    ) {
        BetTitleKey key = new BetTitleKey(betTitleCode, isNot);
        OddsEventMarketsDto markets = getMarketsForMatchSchedule(matchScheduleId);
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

    private OddsEventMarketsDto toDto(
            MatchSchedule match,
            List<OddsMarketGroup> groups,
            Instant fetchedAt,
            List<String> bookmakers
    ) {
        Instant kickoffUtc = match.getUtcKickoff();
        return OddsEventMarketsDto.builder()
                .matchScheduleId(match.getId())
                .homeTeamId(match.getHomeTeamId())
                .awayTeamId(match.getAwayTeamId())
                .status(match.getStatus())
                .kickoffUtc(kickoffUtc)
                .fetchedAt(fetchedAt)
                .bookmakers(bookmakers)
                .marketGroups(groups)
                .build();
    }

    public record OddsLineSelection(
            String category,
            net.friendly_bets.models.odds.OddsLineRow row,
            String odds
    ) {
    }
}
