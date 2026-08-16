package net.friendly_bets.flashscoreua;

import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.providers.standings.StandingRowSnapshot;
import net.friendly_bets.providers.standings.StandingZoneRuleSnapshot;
import net.friendly_bets.providers.standings.StandingsTableSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Livesport {@code to_{tournament}_{stage}_{table}} standings feed
 * ({@code KEY÷value¬} records separated by {@code ~}).
 */
@Component
public class FlashscoreUaStandingsParser {

    private static final String LOGO_BASE = "https://static.flashscore.com/res/image/data/";

    public StandingsTableSnapshot parse(String feed, String sourceUrl) {
        if (feed == null || feed.isBlank() || "0".equals(feed.trim())) {
            throw new BadRequestException("flashscoreUaStandingsEmpty");
        }
        List<String> records = splitRecords(feed);
        Map<String, StandingZoneRuleSnapshot> zoneRules = parseLegend(records);
        Map<String, String> logosByParticipantId = new LinkedHashMap<>();
        List<StandingRowSnapshot> rows = new ArrayList<>();
        String pendingParticipantId = null;
        for (String record : records) {
            Map<String, String> fields = parseRecord(record);
            String participantId = blankToNull(fields.get("IPI"));
            String logoFile = blankToNull(fields.get("IPU"));
            if (participantId != null) {
                pendingParticipantId = participantId;
            }
            if (logoFile != null) {
                String logoOwner = participantId != null ? participantId : pendingParticipantId;
                if (logoOwner != null) {
                    logosByParticipantId.put(logoOwner, logoFile);
                }
                pendingParticipantId = null;
            }
            if (!fields.containsKey("TR") || !fields.containsKey("TN")) {
                continue;
            }
            StandingRowSnapshot row = parseRow(fields, zoneRules, logosByParticipantId);
            if (row != null) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            throw new BadRequestException("flashscoreUaStandingsEmpty");
        }
        return StandingsTableSnapshot.builder()
                .sourceUrl(sourceUrl)
                .rows(rows)
                .zoneRules(new ArrayList<>(zoneRules.values()))
                .build();
    }

    /**
     * Legend entries {@code TV÷code|label|hex} — several TV keys can share one record.
     * Parsed before rows so provider wording wins over a raw zone code.
     */
    static Map<String, StandingZoneRuleSnapshot> parseLegend(List<String> records) {
        Map<String, StandingZoneRuleSnapshot> zoneRules = new LinkedHashMap<>();
        if (records == null) {
            return zoneRules;
        }
        for (String record : records) {
            if (record == null || record.isBlank()) {
                continue;
            }
            for (String segment : record.split("¬")) {
                if (segment == null || !segment.startsWith("TV÷") || segment.length() < 5) {
                    continue;
                }
                applyLegendEntry(segment.substring(3), zoneRules);
            }
        }
        return zoneRules;
    }

    private static void applyLegendEntry(String raw, Map<String, StandingZoneRuleSnapshot> zoneRules) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] parts = raw.split("\\|", 3);
        if (parts.length < 2) {
            return;
        }
        String code = blankToNull(parts[0]);
        String label = blankToNull(parts[1]);
        if (code == null) {
            return;
        }
        String color = parts.length >= 3 ? normalizeHex(parts[2]) : null;
        StandingZoneRuleSnapshot existing = zoneRules.get(code);
        if (existing == null) {
            zoneRules.put(code, StandingZoneRuleSnapshot.builder()
                    .code(code)
                    .label(label != null ? label : code)
                    .color(color)
                    .build());
            return;
        }
        if (label != null) {
            existing.setLabel(label);
        }
        if (color != null) {
            existing.setColor(color);
        }
    }

    private StandingRowSnapshot parseRow(
            Map<String, String> fields,
            Map<String, StandingZoneRuleSnapshot> zoneRules,
            Map<String, String> logosByParticipantId
    ) {
        String teamName = blankToNull(fields.get("TN"));
        if (teamName == null) {
            return null;
        }
        int[] goals = parseGoals(fields.get("TG"));
        String zoneCode = blankToNull(fields.get("TU"));
        String zoneColor = normalizeHex(fields.get("TUC"));
        if (zoneCode != null) {
            zoneRules.putIfAbsent(zoneCode, StandingZoneRuleSnapshot.builder()
                    .code(zoneCode)
                    .label(zoneCode)
                    .color(zoneColor)
                    .build());
        }
        String participantId = blankToNull(fields.get("TI"));
        String logoFile = participantId != null ? logosByParticipantId.get(participantId) : null;
        return StandingRowSnapshot.builder()
                .rank(parseInt(fields.get("TR")))
                .externalTeamName(teamName)
                .logoUrl(logoFile != null ? LOGO_BASE + logoFile : null)
                .played(parseInt(fields.get("TM")))
                .wins(parseInt(fields.get("TW")))
                .draws(parseInt(fields.get("TDR")))
                .losses(parseInt(fields.get("TL")))
                .goalsFor(goals[0])
                .goalsAgainst(goals[1])
                .goalDifference(goals[0] - goals[1])
                .points(parseInt(fields.get("TP")))
                .zoneCode(zoneCode)
                .build();
    }

    static List<String> splitRecords(String feed) {
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

    static Map<String, String> parseRecord(String record) {
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

    private static int[] parseGoals(String text) {
        if (text == null || text.isBlank()) {
            return new int[] {0, 0};
        }
        String[] parts = text.trim().split(":");
        if (parts.length != 2) {
            return new int[] {0, 0};
        }
        return new int[] {parseInt(parts[0]), parseInt(parts[1])};
    }

    private static int parseInt(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String digits = text.replaceAll("[^0-9-]", "").trim();
        if (digits.isEmpty() || "-".equals(digits)) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private static String normalizeHex(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        return "#" + hex.toUpperCase();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace('\u00a0', ' ').trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
