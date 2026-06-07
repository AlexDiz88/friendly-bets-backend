package net.friendly_bets.oddsapi.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class OddsRawRowFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OddsRawRowFormatter() {
    }

    public static String fromJsonNode(JsonNode row) {
        if (row == null || row.isNull()) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(row);
        } catch (Exception e) {
            return row.toString();
        }
    }

    public static String fromParsedLine(String marketName, String line, Map<String, String> prices) {
        Map<String, String> parts = new TreeMap<>();
        if (marketName != null && !marketName.isBlank()) {
            parts.put("market", marketName.trim());
        }
        if (line != null && !line.isBlank()) {
            parts.put("hdp", line);
        }
        if (prices != null) {
            prices.forEach((k, v) -> {
                if (k != null && v != null) {
                    parts.put(k, v);
                }
            });
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = parts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }
}
