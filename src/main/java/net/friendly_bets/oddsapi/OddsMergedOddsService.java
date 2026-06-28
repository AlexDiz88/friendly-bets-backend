package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.gameresults.GameResultRecord;
import net.friendly_bets.models.odds.GameResultMergedOdds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMappingPipeline;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.oddsapi.mapping.OddsProductionMergeFilter;
import net.friendly_bets.repositories.GameResultMergedOddsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Персист merged odds: {@link OddsMappingPipeline} (маппинг по БК → мерж → best odds).
 */
@Service
@RequiredArgsConstructor
public class OddsMergedOddsService {

    private final GameResultMergedOddsRepository mergedOddsRepository;
    private final OddsMappingPipeline oddsMappingPipeline;

    public OddsMergeResult buildAndPersist(
            GameResultRecord match,
            Map<String, List<net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower,
            OddsMatchContext matchContext,
            List<String> bookmakers,
            LocalDateTime fetchedAt,
            boolean frozen
    ) {
        OddsMergeResult mergeResult = oddsMappingPipeline.build(bookmakerMarkets, canonicalByLower, matchContext);
        List<OddsMarketGroup> groups = mergeResult.getMarketGroups();
        OddsSelectionKey.enrichGroups(groups);
        enrichBetTitles(groups);
        OddsResultTotalEnricher.appendCalculatedGroups(groups, bookmakers);
        OddsHalfCorrectScoreSubgroupSplitter.splitIntoSubgroups(groups);
        OddsPeriodHandicapSubgroupSplitter.splitIntoSubgroups(groups);
        OddsResultTotalEnricher.applyCategoryMetadata(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
        OddsMerger.sortMarketGroupRows(groups);

        if (match == null || match.getId() == null) {
            return mergeResult;
        }

        Optional<GameResultMergedOdds> existing = mergedOddsRepository.findByGameResultId(match.getId());
        if (existing.isPresent() && existing.get().getFrozenAt() != null) {
            return mergeResult;
        }

        persistMergedSnapshot(match.getId(), bookmakers, groups, fetchedAt, frozen);
        return mergeResult;
    }

    public Optional<GameResultMergedOdds> findByGameResultId(String gameResultId) {
        return mergedOddsRepository.findByGameResultId(gameResultId);
    }

    /**
     * Persist merged odds from pre-mapped quotes (e.g. Marathonbet scrape).
     */
    public OddsMergeResult buildAndPersistFromQuotes(
            GameResultRecord match,
            List<MappedOddsQuote> quotes,
            List<String> bookmakers,
            LocalDateTime fetchedAt,
            boolean frozen
    ) {
        List<MappedOddsQuote> prodMergeInput = new ArrayList<>();
        if (quotes != null) {
            for (MappedOddsQuote quote : quotes) {
                if (!quote.isOk() || OddsProductionMergeFilter.includeInProductionMerge(quote)) {
                    prodMergeInput.add(quote);
                }
            }
        }
        OddsMergeResult mergeResult = OddsMerger.merge(prodMergeInput, false);
        List<OddsMarketGroup> groups = mergeResult.getMarketGroups();
        OddsSelectionKey.enrichGroups(groups);
        enrichBetTitles(groups);
        OddsResultTotalEnricher.appendCalculatedGroups(groups, bookmakers);
        OddsHalfCorrectScoreSubgroupSplitter.splitIntoSubgroups(groups);
        OddsPeriodHandicapSubgroupSplitter.splitIntoSubgroups(groups);
        OddsResultTotalEnricher.applyCategoryMetadata(groups);
        OddsLineRowDeduper.dedupeMarketGroups(groups);
        OddsMerger.sortMarketGroupRows(groups);

        if (match == null || match.getId() == null) {
            return mergeResult;
        }

        Optional<GameResultMergedOdds> existing = mergedOddsRepository.findByGameResultId(match.getId());
        if (existing.isPresent() && existing.get().getFrozenAt() != null) {
            return mergeResult;
        }

        persistMergedSnapshot(match.getId(), bookmakers, groups, fetchedAt, frozen);
        return mergeResult;
    }

    public void freezeIfNeeded(GameResultRecord match, LocalDateTime now) {
        if (match == null || match.getId() == null || GameResultNotStarted.isNotStarted(match, now)) {
            return;
        }
        mergedOddsRepository.findByGameResultId(match.getId()).ifPresent(doc -> {
            if (doc.getFrozenAt() == null) {
                doc.setFrozenAt(now);
                mergedOddsRepository.save(doc);
            }
        });
    }

    /**
     * Один документ на матч: полная замена market_groups (не append).
     */
    private void persistMergedSnapshot(
            String gameResultId,
            List<String> bookmakers,
            List<OddsMarketGroup> groups,
            LocalDateTime fetchedAt,
            boolean frozen
    ) {
        GameResultMergedOdds entity = mergedOddsRepository.findByGameResultId(gameResultId)
                .orElse(GameResultMergedOdds.builder()
                        .gameResultId(gameResultId)
                        .build());
        entity.setFetchedAt(fetchedAt);
        entity.setBookmakers(bookmakers != null ? new ArrayList<>(bookmakers) : List.of());
        entity.setMarketGroups(new ArrayList<>(groups));
        if (frozen) {
            entity.setFrozenAt(fetchedAt);
        }
        mergedOddsRepository.save(entity);
    }

    /** Подписи строк и betTitle для UI (в т.ч. при чтении снимка из MongoDB). */
    public void enrichBetTitles(List<OddsMarketGroup> groups) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            enrichBetTitlesForGroup(group);
        }
    }

    private void enrichBetTitlesForGroup(OddsMarketGroup group) {
        if (group == null) {
            return;
        }
        if (group.getRows() != null && group.getCategory() != null) {
            try {
                OddsMarketCategory category = OddsMarketCategory.valueOf(group.getCategory());
                List<net.friendly_bets.models.odds.OddsLineRow> bettable = new ArrayList<>();
                for (var row : group.getRows()) {
                    if (row.getBetTitle() != null) {
                        if (row.getDisplayLabel() == null || row.getDisplayLabel().isBlank()) {
                            row.setDisplayLabel(OddsDisplayLabelFormatter.format(category, row));
                        }
                        bettable.add(row);
                        continue;
                    }
                    try {
                        row.setBetTitle(OddsSelectionBetTitleMapper.toBetTitle(group.getCategory(), row));
                        row.setDisplayLabel(OddsDisplayLabelFormatter.format(category, row));
                        bettable.add(row);
                    } catch (Exception ignored) {
                        row.setBetTitle(null);
                    }
                }
                group.setRows(bettable);
            } catch (IllegalArgumentException ignored) {
                // напр. RESULT_TOTAL-родитель без строк
            }
        }
        if (group.getSubgroups() != null) {
            for (OddsMarketGroup sub : group.getSubgroups()) {
                enrichBetTitlesForGroup(sub);
            }
        }
    }
}
