package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OddsGroupBuilder {

    private OddsGroupBuilder() {
    }

    public static List<OddsMarketGroup> build(
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower,
            OddsMatchContext match
    ) {
        Map<OddsMarketCategory, Map<String, OddsLineRow>> rowsByCategory = new TreeMap<>(
                Comparator.comparingInt(OddsMarketCategory::getSortOrder));

        if (bookmakerMarkets == null) {
            return List.of();
        }

        for (Map.Entry<String, List<OddsApiMarketDto>> entry : bookmakerMarkets.entrySet()) {
            String bookmaker = OddsBookmakerKeys.resolveCanonical(entry.getKey(), canonicalByLower);
            if (bookmaker == null) {
                continue;
            }
            List<ParsedOddsMarket> parsed = OddsMarketParser.parseAndFilter(entry.getValue());
            for (ParsedOddsMarket market : parsed) {
                OddsMarketCategory category = OddsMarketCatalog.resolveCategory(market.getName());
                if (category == OddsMarketCategory.EXCLUDED) {
                    continue;
                }
                Map<String, OddsLineRow> categoryRows = rowsByCategory.computeIfAbsent(
                        category, k -> new LinkedHashMap<>());

                for (ParsedOddsMarket.ParsedOddsLine line : market.getLines()) {
                    if (category == OddsMarketCategory.CORRECT_SCORE || category == OddsMarketCategory.OTHER) {
                        mergeRawSelectionRows(
                                categoryRows, category, market.getName(), line, bookmaker);
                        continue;
                    }
                    Map<OddsSelectionCode, String> normalized = new LinkedHashMap<>();
                    OddsSelectionNormalizer.normalizeLinePrices(
                            category, line.getPrices(), match, normalized);

                    for (Map.Entry<OddsSelectionCode, String> price : normalized.entrySet()) {
                        String key = rowKey(category, line.getLine(), price.getKey().name());
                        OddsLineRow row = categoryRows.computeIfAbsent(key, k -> OddsLineRow.builder()
                                .line(categoryUsesLine(category) ? line.getLine() : null)
                                .selectionCode(price.getKey().name())
                                .displayLabel(price.getKey().displayLabel())
                                .bookmakerOdds(new LinkedHashMap<>())
                                .build());
                        row.getBookmakerOdds().put(bookmaker, price.getValue());
                    }
                }
            }
        }

        List<OddsMarketGroup> groups = new ArrayList<>();
        for (Map.Entry<OddsMarketCategory, Map<String, OddsLineRow>> entry : rowsByCategory.entrySet()) {
            OddsMarketCategory category = entry.getKey();
            List<OddsLineRow> rows = new ArrayList<>(entry.getValue().values());
            rows.sort(rowComparator(category));
            groups.add(OddsMarketGroup.builder()
                    .category(category.name())
                    .groupKey(OddsMarketCatalog.i18nGroupKey(category))
                    .sortOrder(category.getSortOrder())
                    .collapsedByDefault(category.isCollapsedByDefault())
                    .rows(rows)
                    .build());
        }
        return groups;
    }

    private static void mergeRawSelectionRows(
            Map<String, OddsLineRow> categoryRows,
            OddsMarketCategory category,
            String marketName,
            ParsedOddsMarket.ParsedOddsLine line,
            String bookmaker
    ) {
        for (Map.Entry<String, String> entry : line.getPrices().entrySet()) {
            String selection = entry.getKey();
            if (selection == null || selection.isBlank()) {
                continue;
            }
            String key = category == OddsMarketCategory.OTHER
                    ? otherRowKey(marketName, selection)
                    : rowKey(category, line.getLine(), selection);
            String displayLabel = category == OddsMarketCategory.OTHER
                    ? formatOtherDisplayLabel(marketName, selection)
                    : formatCorrectScoreLabel(selection);
            OddsLineRow row = categoryRows.computeIfAbsent(key, k -> OddsLineRow.builder()
                    .line(categoryUsesLine(category) ? line.getLine() : null)
                    .selectionCode(selection)
                    .displayLabel(displayLabel)
                    .bookmakerOdds(new LinkedHashMap<>())
                    .build());
            row.getBookmakerOdds().put(bookmaker, entry.getValue());
        }
    }

    private static String formatCorrectScoreLabel(String selection) {
        return selection.trim();
    }

    private static String formatOtherDisplayLabel(String marketName, String selection) {
        if (marketName == null || marketName.isBlank()) {
            return selection.trim();
        }
        return marketName.trim() + " · " + selection.trim();
    }

    private static boolean categoryUsesLine(OddsMarketCategory category) {
        return category == OddsMarketCategory.HANDICAP
                || category == OddsMarketCategory.TOTALS
                || category == OddsMarketCategory.TEAM_TOTAL_HOME
                || category == OddsMarketCategory.TEAM_TOTAL_AWAY;
    }

    private static String otherRowKey(String marketName, String selection) {
        String market = marketName == null || marketName.isBlank()
                ? ""
                : marketName.trim().toLowerCase(java.util.Locale.ROOT);
        return market + "|" + selection.trim();
    }

    private static String rowKey(OddsMarketCategory category, String line, String selection) {
        if (!categoryUsesLine(category)) {
            return selection;
        }
        String linePart = line == null || line.isBlank() ? "" : line.trim();
        return linePart + "|" + selection;
    }

    private static Comparator<OddsLineRow> rowComparator(OddsMarketCategory category) {
        if (category == OddsMarketCategory.CORRECT_SCORE) {
            return Comparator.comparingInt(r -> OddsCorrectScoreUtils.sortKey(r.getSelectionCode()));
        }
        return Comparator
                .comparingDouble((OddsLineRow r) -> parseLine(r.getLine()))
                .thenComparingInt(r -> selectionOrder(category, r.getSelectionCode()));
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

    private static int selectionOrder(OddsMarketCategory category, String selectionCode) {
        if (selectionCode == null) {
            return 99;
        }
        try {
            return OddsSelectionCode.valueOf(selectionCode).orderWithinGroup(category);
        } catch (IllegalArgumentException e) {
            return 99;
        }
    }
}
