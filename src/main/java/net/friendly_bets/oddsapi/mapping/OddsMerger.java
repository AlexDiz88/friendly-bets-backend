package net.friendly_bets.oddsapi.mapping;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.OddsDisplayLabelFormatter;
import net.friendly_bets.oddsapi.OddsMarketCatalog;
import net.friendly_bets.oddsapi.OddsMarketCategory;
import net.friendly_bets.oddsapi.OddsSelectionKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Этап 2: объединение котировок по {@link BetTitleKey} (code + isNot) после маппинга каждой БК.
 * Объединение линий — по сути union: каждый уникальный BetTitle из любой БК попадает в итог;
 * если тот же BetTitle есть у нескольких БК — одна строка, колонки по букмекерам, best odds.
 */
public final class OddsMerger {

    private OddsMerger() {
    }

    public static OddsMergeResult merge(List<MappedOddsQuote> quotes) {
        List<MappedOddsQuote> rejected = new ArrayList<>();
        List<MappedOddsQuote> okQuotes = new ArrayList<>();
        if (quotes != null) {
            for (MappedOddsQuote quote : quotes) {
                if (quote.isOk()) {
                    okQuotes.add(quote);
                } else if (quote.getMappingStatus() != OddsMappingStatus.OK) {
                    rejected.add(quote);
                }
            }
        }

        Map<BetTitleKey, List<MappedOddsQuote>> byBetTitle = new LinkedHashMap<>();
        for (MappedOddsQuote quote : okQuotes) {
            BetTitleKey key = quote.betTitleKey();
            if (key == null) {
                continue;
            }
            byBetTitle.computeIfAbsent(key, k -> new ArrayList<>()).add(quote);
        }

        List<OddsCrossBookmakerMismatch> mismatches = new ArrayList<>();
        Map<OddsMarketCategory, Map<BetTitleKey, OddsLineRow>> rowsByCategory = new TreeMap<>(
                Comparator.comparingInt(OddsMarketCategory::getSortOrder));

        for (Map.Entry<BetTitleKey, List<MappedOddsQuote>> entry : byBetTitle.entrySet()) {
            BetTitleKey key = entry.getKey();
            List<MappedOddsQuote> group = entry.getValue();
            Map<String, String> bookmakerOdds = mergeBookmakerOdds(group);
            if (OddsMappingValidator.isCrossBookmakerMismatch(bookmakerOdds)) {
                OddsCrossBookmakerMismatch mismatch = OddsMappingValidator.firstMismatch(key, group);
                if (mismatch != null) {
                    mismatches.add(mismatch);
                }
                // Fail-closed: при расхождении >50% в UI не попадает ни один кэф из группы.
                for (MappedOddsQuote q : group) {
                    rejected.add(MappedOddsQuote.builder()
                            .bookmaker(q.getBookmaker())
                            .marketName(q.getMarketName())
                            .rawRowJson(q.getRawRowJson())
                            .category(q.getCategory())
                            .betTitle(q.getBetTitle())
                            .odds(q.getOdds())
                            .mappingStatus(OddsMappingStatus.REJECTED)
                            .rejectReason(OddsRejectReason.CROSS_BOOKMAKER_MISMATCH)
                            .rejectDetail("odds diverge across bookmakers")
                            .selectionCode(q.getSelectionCode())
                            .line(q.getLine())
                            .build());
                }
                continue;
            }

            MappedOddsQuote first = group.get(0);
            OddsMarketCategory category = first.getCategory();
            BetTitle betTitle = first.getBetTitle();
            OddsLineRow row = OddsLineRow.builder()
                    .line(first.getLine())
                    .selectionCode(first.getSelectionCode())
                    .betTitle(betTitle)
                    .bookmakerOdds(bookmakerOdds)
                    .build();
            row.setDisplayLabel(OddsDisplayLabelFormatter.format(category, row));
            // Best odds = max кэф среди БК в этой группе (после успешной сверки).
            OddsSelectionKey.applyBestOdds(row, category);

            rowsByCategory
                    .computeIfAbsent(category, c -> new LinkedHashMap<>())
                    .put(key, row);
        }

        List<OddsMarketGroup> groups = new ArrayList<>();
        for (Map.Entry<OddsMarketCategory, Map<BetTitleKey, OddsLineRow>> entry : rowsByCategory.entrySet()) {
            OddsMarketCategory category = entry.getKey();
            List<OddsLineRow> rows = new ArrayList<>(entry.getValue().values());
            sortRows(category, rows);
            groups.add(OddsMarketGroup.builder()
                    .category(category.name())
                    .groupKey(OddsMarketCatalog.i18nGroupKey(category))
                    .sortOrder(category.getSortOrder())
                    .collapsedByDefault(category.isCollapsedByDefault())
                    .rows(rows)
                    .build());
        }

        OddsSelectionKey.enrichGroups(groups);

        return OddsMergeResult.builder()
                .marketGroups(groups)
                .allQuotes(quotes != null ? quotes : List.of())
                .rejectedQuotes(rejected)
                .mismatches(mismatches)
                .build();
    }

    private static void sortRows(OddsMarketCategory category, List<OddsLineRow> rows) {
        rows.sort(rowComparator(category));
    }

    private static Comparator<OddsLineRow> rowComparator(OddsMarketCategory category) {
        if (category == OddsMarketCategory.HANDICAP) {
            return Comparator
                    .comparingDouble((OddsLineRow r) -> Math.abs(parseLine(r.getLine())))
                    .thenComparingDouble(r -> parseLine(r.getLine()))
                    .thenComparingInt(r -> selectionOrder(category, r.getSelectionCode()));
        }
        if (category == OddsMarketCategory.TOTALS
                || category == OddsMarketCategory.TEAM_TOTAL_HOME
                || category == OddsMarketCategory.TEAM_TOTAL_AWAY) {
            return Comparator
                    .comparingDouble((OddsLineRow r) -> parseLine(r.getLine()))
                    .thenComparingInt(r -> selectionOrder(category, r.getSelectionCode()));
        }
        return Comparator.comparingInt(r -> selectionOrder(category, r.getSelectionCode()));
    }

    private static double parseLine(String line) {
        if (line == null || line.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(line.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Одна БК — один кэф на BetTitle; при дублях (Spread + Asian Handicap) — больший. */
    private static Map<String, String> mergeBookmakerOdds(List<MappedOddsQuote> group) {
        Map<String, String> bookmakerOdds = new LinkedHashMap<>();
        for (MappedOddsQuote q : group) {
            String bk = q.getBookmaker();
            String odds = q.getOdds();
            String existing = bookmakerOdds.get(bk);
            if (existing == null || parseOddsValue(odds) > parseOddsValue(existing)) {
                bookmakerOdds.put(bk, odds);
            }
        }
        return bookmakerOdds;
    }

    private static double parseOddsValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int selectionOrder(OddsMarketCategory category, String selectionCode) {
        if (selectionCode == null) {
            return 99;
        }
        String base = category == OddsMarketCategory.BTTS
                ? net.friendly_bets.oddsapi.OddsBttsScope.baseSelectionCode(selectionCode)
                : selectionCode;
        try {
            return net.friendly_bets.oddsapi.OddsSelectionCode.valueOf(base).orderWithinGroup(category);
        } catch (IllegalArgumentException e) {
            return 99;
        }
    }
}
