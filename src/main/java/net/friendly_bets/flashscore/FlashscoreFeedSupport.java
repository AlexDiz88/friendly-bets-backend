package net.friendly_bets.flashscore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Flashscore proprietary feed text ({@code KEY÷value¬} records separated by {@code ~}).
 */
public final class FlashscoreFeedSupport {

    private FlashscoreFeedSupport() {
    }

    public static List<String> splitRecords(String feed) {
        if (feed == null || feed.isBlank()) {
            return List.of();
        }
        List<String> records = new ArrayList<>();
        for (String part : feed.split("~")) {
            if (part != null && !part.isBlank()) {
                records.add(part);
            }
        }
        return records;
    }

    public static Map<String, String> parseRecord(String record) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (record == null || record.isBlank()) {
            return fields;
        }
        for (String segment : record.split("¬")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            int idx = segment.indexOf('÷');
            if (idx <= 0 || idx >= segment.length() - 1) {
                continue;
            }
            fields.put(segment.substring(0, idx), segment.substring(idx + 1));
        }
        return fields;
    }

    public static String firstNonBlank(Map<String, String> fields, String... keys) {
        if (fields == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = fields.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
