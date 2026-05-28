package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.JsonNode;
import net.friendly_bets.models.odds.OddsMarket;
import net.friendly_bets.models.odds.OddsOutcome;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class OddsApiMarketMapper {

    private OddsApiMarketMapper() {
    }

    public static List<OddsMarket> toMarkets(List<OddsApiMarketDto> fromApi) {
        List<ParsedOddsMarket> parsed = OddsMarketParser.parseAndFilter(fromApi);
        List<OddsMarket> markets = new ArrayList<>();
        for (ParsedOddsMarket market : parsed) {
            markets.add(OddsMarket.builder()
                    .name(market.getName())
                    .outcomes(toOutcomes(market.getLines()))
                    .build());
        }
        return markets;
    }

    private static List<OddsOutcome> toOutcomes(List<ParsedOddsMarket.ParsedOddsLine> lines) {
        List<OddsOutcome> outcomes = new ArrayList<>();
        for (ParsedOddsMarket.ParsedOddsLine line : lines) {
            for (Map.Entry<String, String> price : line.getPrices().entrySet()) {
                outcomes.add(OddsOutcome.builder()
                        .label(price.getKey())
                        .odds(price.getValue())
                        .line(line.getLine())
                        .build());
            }
        }
        return outcomes;
    }

    static List<OddsOutcome> parseOutcomes(List<JsonNode> oddsRows) {
        if (oddsRows == null || oddsRows.isEmpty()) {
            return List.of();
        }
        List<OddsOutcome> outcomes = new ArrayList<>();
        for (JsonNode row : oddsRows) {
            if (row == null || row.isNull()) {
                continue;
            }
            if (row.isObject()) {
                appendObjectOutcomes(outcomes, row);
            } else if (row.isArray()) {
                for (JsonNode item : row) {
                    if (item != null && item.isObject()) {
                        appendObjectOutcomes(outcomes, item);
                    }
                }
            }
        }
        return outcomes;
    }

    private static void appendObjectOutcomes(List<OddsOutcome> outcomes, JsonNode row) {
        String line = textOrNull(row.get("hdp"));
        if (line == null) {
            line = textOrNull(row.get("handicap"));
        }
        if (line == null) {
            line = textOrNull(row.get("max"));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (isMetaKey(key)) {
                continue;
            }
            String value = textOrNull(field.getValue());
            if (value == null) {
                continue;
            }
            outcomes.add(OddsOutcome.builder()
                    .label(key)
                    .odds(value)
                    .line(line)
                    .build());
        }
    }

    private static boolean isMetaKey(String key) {
        return "hdp".equals(key) || "handicap".equals(key) || "max".equals(key);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asText();
        }
        return null;
    }
}
