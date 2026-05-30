package net.friendly_bets.oddsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.friendly_bets.models.odds.OddsMarket;
import net.friendly_bets.models.odds.OddsOutcome;
import net.friendly_bets.oddsapi.client.dto.OddsApiMarketDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds API-shaped markets from persisted {@link OddsMarket} for presentation cache hits.
 */
public final class OddsStoredMarketsConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OddsStoredMarketsConverter() {
    }

    public static List<OddsApiMarketDto> toApiMarkets(List<OddsMarket> markets) {
        if (markets == null || markets.isEmpty()) {
            return List.of();
        }
        List<OddsApiMarketDto> result = new ArrayList<>();
        for (OddsMarket market : markets) {
            if (market == null || market.getName() == null) {
                continue;
            }
            OddsApiMarketDto dto = new OddsApiMarketDto();
            dto.setName(market.getName());
            dto.setOdds(buildOddsNodes(market.getOutcomes()));
            result.add(dto);
        }
        return result;
    }

    private static List<com.fasterxml.jackson.databind.JsonNode> buildOddsNodes(List<OddsOutcome> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) {
            return List.of();
        }
        Map<String, ObjectNode> byLine = new LinkedHashMap<>();
        for (OddsOutcome outcome : outcomes) {
            if (outcome == null || outcome.getLabel() == null || outcome.getOdds() == null) {
                continue;
            }
            String lineKey = outcome.getLine() != null ? outcome.getLine() : "";
            ObjectNode row = byLine.computeIfAbsent(lineKey, k -> {
                ObjectNode node = MAPPER.createObjectNode();
                if (!k.isBlank()) {
                    node.put("hdp", k);
                }
                return node;
            });
            row.put(outcome.getLabel(), outcome.getOdds());
        }
        ArrayNode array = MAPPER.createArrayNode();
        byLine.values().forEach(array::add);
        List<com.fasterxml.jackson.databind.JsonNode> list = new ArrayList<>();
        array.forEach(list::add);
        return list;
    }
}
