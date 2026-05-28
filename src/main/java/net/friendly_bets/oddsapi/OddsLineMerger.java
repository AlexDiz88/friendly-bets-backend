package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.MergedOddsLine;
import net.friendly_bets.models.odds.MergedOddsSelection;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OddsLineMerger {

    private OddsLineMerger() {
    }

    /**
     * @param bookmakerMarkets ключ API букмекера → список рынков
     * @param canonicalByLower lower-case → canonical name from config
     */
    public static List<MergedOddsLine> merge(
            Map<String, List<OddsApiMarketDto>> bookmakerMarkets,
            Map<String, String> canonicalByLower
    ) {
        Map<String, MergedOddsLine> byKey = new LinkedHashMap<>();

        if (bookmakerMarkets == null) {
            return List.of();
        }

        for (Map.Entry<String, List<OddsApiMarketDto>> entry : bookmakerMarkets.entrySet()) {
            String canonical = OddsBookmakerKeys.resolveCanonical(entry.getKey(), canonicalByLower);
            if (canonical == null) {
                continue;
            }
            List<ParsedOddsMarket> parsed = OddsMarketParser.parseAndFilter(entry.getValue());
            for (ParsedOddsMarket market : parsed) {
                for (ParsedOddsMarket.ParsedOddsLine line : market.getLines()) {
                    String mergeKey = mergeKey(market.getName(), line.getLine());
                    MergedOddsLine merged = byKey.computeIfAbsent(mergeKey, k -> MergedOddsLine.builder()
                            .marketName(market.getName())
                            .line(line.getLine())
                            .selections(new ArrayList<>())
                            .build());

                    for (Map.Entry<String, String> price : line.getPrices().entrySet()) {
                        MergedOddsSelection selection = findOrCreateSelection(merged, price.getKey());
                        selection.getBookmakerOdds().put(canonical, price.getValue());
                    }
                }
            }
        }

        return new ArrayList<>(byKey.values());
    }

    private static String mergeKey(String marketName, String line) {
        String base = marketName.trim().toLowerCase(Locale.ROOT);
        if (line == null || line.isBlank()) {
            return base;
        }
        return base + "|" + line.trim();
    }

    private static MergedOddsSelection findOrCreateSelection(MergedOddsLine merged, String selectionKey) {
        for (MergedOddsSelection existing : merged.getSelections()) {
            if (selectionKey.equals(existing.getSelectionKey())) {
                return existing;
            }
        }
        MergedOddsSelection created = MergedOddsSelection.builder()
                .selectionKey(selectionKey)
                .displayLabel(displayLabel(selectionKey))
                .bookmakerOdds(new LinkedHashMap<>())
                .build();
        merged.getSelections().add(created);
        return created;
    }

    private static String displayLabel(String selectionKey) {
        return switch (selectionKey) {
            case "home" -> "П1";
            case "away" -> "П2";
            case "draw" -> "Х";
            case "over" -> "Б";
            case "under" -> "М";
            case "yes" -> "Да";
            case "no" -> "Нет";
            case "1x" -> "1X";
            case "12" -> "12";
            case "x2" -> "X2";
            default -> selectionKey;
        };
    }
}
