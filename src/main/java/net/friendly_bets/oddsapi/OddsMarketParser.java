package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OddsMarketParser {

    private OddsMarketParser() {
    }

    public static List<ParsedOddsMarket> parseAndFilter(List<OddsApiMarketDto> fromApi) {
        if (fromApi == null || fromApi.isEmpty()) {
            return List.of();
        }
        List<ParsedOddsMarket> result = new ArrayList<>();
        for (OddsApiMarketDto dto : fromApi) {
            if (dto == null || dto.getName() == null) {
                continue;
            }
            if (!OddsMarketFilter.isMarketAllowed(dto.getName())) {
                continue;
            }
            List<ParsedOddsMarket.ParsedOddsLine> lines = parseLines(dto.getOdds());
            if (lines.isEmpty()) {
                continue;
            }
            result.add(ParsedOddsMarket.builder()
                    .name(dto.getName().trim())
                    .updatedAt(dto.getUpdatedAt())
                    .lines(lines)
                    .build());
        }
        return result;
    }

    private static List<ParsedOddsMarket.ParsedOddsLine> parseLines(List<JsonNode> oddsRows) {
        List<ParsedOddsMarket.ParsedOddsLine> lines = new ArrayList<>();
        if (oddsRows == null) {
            return lines;
        }
        for (JsonNode row : oddsRows) {
            if (row == null || row.isNull() || !row.isObject()) {
                continue;
            }
            boolean labelRow = row.hasNonNull("label");
            String line = labelRow ? null : extractLine(row);
            if (!OddsMarketFilter.isLineAllowed(line)) {
                continue;
            }
            Map<String, String> prices = extractPrices(row);
            if (prices.isEmpty()) {
                continue;
            }
            lines.add(ParsedOddsMarket.ParsedOddsLine.builder()
                    .line(line)
                    .prices(prices)
                    .build());
        }
        return lines;
    }

    private static String extractLine(JsonNode row) {
        JsonNode hdp = row.get("hdp");
        if (hdp == null || hdp.isNull()) {
            hdp = row.get("handicap");
        }
        if (hdp == null || hdp.isNull()) {
            return null;
        }
        return hdp.isNumber() ? String.valueOf(hdp.asDouble()) : hdp.asText();
    }

    private static Map<String, String> extractPrices(JsonNode row) {
        Map<String, String> prices = new LinkedHashMap<>();
        if (row.hasNonNull("label")) {
            String label = row.get("label").asText();
            String odds = firstOddsValue(row);
            if (label != null && !label.isBlank() && odds != null) {
                prices.put(normalizeSelectionKey(label), odds);
            }
            return prices;
        }
        row.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (isMetaKey(key)) {
                return;
            }
            String value = textOdds(entry.getValue());
            if (value != null) {
                prices.put(normalizeSelectionKey(key), value);
            }
        });
        return prices;
    }

    private static String firstOddsValue(JsonNode row) {
        for (String key : List.of("under", "over", "odds", "home", "away", "yes", "no", "draw")) {
            String v = textOdds(row.get(key));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String normalizeSelectionKey(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isMetaKey(String key) {
        return "hdp".equals(key) || "handicap".equals(key) || "max".equals(key) || "label".equals(key);
    }

    private static String textOdds(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber()) {
            return node.asText();
        }
        return null;
    }
}
