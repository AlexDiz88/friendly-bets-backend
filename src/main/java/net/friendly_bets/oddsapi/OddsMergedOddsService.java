package net.friendly_bets.oddsapi;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.odds.Odds;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.models.schedule.MatchSchedule;
import net.friendly_bets.oddsapi.mapping.MappedOddsQuote;
import net.friendly_bets.oddsapi.mapping.OddsMergeResult;
import net.friendly_bets.oddsapi.mapping.OddsMerger;
import net.friendly_bets.oddsapi.mapping.OddsProductionMergeFilter;
import net.friendly_bets.repositories.OddsRepository;
import net.friendly_bets.services.MatchScheduleDisplayService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persist / read merged odds from Marathonbet scrape ({@code odds} collection).
 */
@Service
@RequiredArgsConstructor
public class OddsMergedOddsService {

    private final OddsRepository oddsRepository;

    public Optional<Odds> findByMatchScheduleId(String matchScheduleId) {
        return oddsRepository.findByMatchScheduleId(matchScheduleId);
    }

    public void deleteByMatchScheduleId(String matchScheduleId) {
        if (matchScheduleId == null || matchScheduleId.isBlank()) {
            return;
        }
        oddsRepository.deleteByMatchScheduleId(matchScheduleId);
    }

    /**
     * Delete odds when the schedule is finalized (FINISHED / score present).
     */
    public void deleteIfFinalized(MatchSchedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            return;
        }
        if (MatchScheduleDisplayService.isFinalized(schedule)) {
            oddsRepository.deleteByMatchScheduleId(schedule.getId());
        }
    }

    /**
     * Persist merged odds from pre-mapped quotes (e.g. Marathonbet scrape).
     */
    public OddsMergeResult buildAndPersistFromQuotes(
            MatchSchedule match,
            List<MappedOddsQuote> quotes,
            List<String> bookmakers,
            LocalDateTime fetchedAt,
            boolean frozen,
            Long marathonbetTreeId
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

        Optional<Odds> existing = oddsRepository.findByMatchScheduleId(match.getId());
        if (existing.isPresent() && existing.get().getFrozenAt() != null) {
            return mergeResult;
        }

        persistMergedSnapshot(match.getId(), bookmakers, groups, fetchedAt, frozen, marathonbetTreeId);
        return mergeResult;
    }

    public void freezeIfNeeded(MatchSchedule match, Instant now) {
        if (match == null || match.getId() == null || MatchScheduleNotStarted.isNotStarted(match, now)) {
            return;
        }
        oddsRepository.findByMatchScheduleId(match.getId()).ifPresent(doc -> {
            if (doc.getFrozenAt() == null) {
                doc.setFrozenAt(LocalDateTime.now());
                oddsRepository.save(doc);
            }
        });
    }

    /**
     * One document per match: full replace of market_groups (not append).
     */
    private void persistMergedSnapshot(
            String matchScheduleId,
            List<String> bookmakers,
            List<OddsMarketGroup> groups,
            LocalDateTime fetchedAt,
            boolean frozen,
            Long marathonbetTreeId
    ) {
        Odds entity = oddsRepository.findByMatchScheduleId(matchScheduleId)
                .orElse(Odds.builder()
                        .matchScheduleId(matchScheduleId)
                        .build());
        entity.setFetchedAt(fetchedAt);
        entity.setBookmakers(bookmakers != null ? new ArrayList<>(bookmakers) : List.of());
        entity.setMarketGroups(new ArrayList<>(groups));
        if (marathonbetTreeId != null && marathonbetTreeId > 0) {
            entity.setMarathonbetTreeId(marathonbetTreeId);
        }
        if (frozen) {
            entity.setFrozenAt(fetchedAt);
        }
        oddsRepository.save(entity);
    }

    /** Row labels and betTitle for UI (including when reading a snapshot from MongoDB). */
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
                // e.g. RESULT_TOTAL parent without rows
            }
        }
        if (group.getSubgroups() != null) {
            for (OddsMarketGroup sub : group.getSubgroups()) {
                enrichBetTitlesForGroup(sub);
            }
        }
    }
}
