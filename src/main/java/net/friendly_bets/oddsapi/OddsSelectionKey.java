package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.Map;

public final class OddsSelectionKey {

    private OddsSelectionKey() {
    }

    public static String build(OddsMarketCategory category, String groupKey, OddsLineRow row) {
        String cat = category != null ? category.name() : "OTHER";
        String gk = groupKey != null ? groupKey : "";
        String line = row.getLine() != null ? row.getLine().trim() : "";
        String sel = row.getSelectionCode() != null ? row.getSelectionCode().trim() : "";
        return cat + "|" + gk + "|" + line + "|" + sel;
    }

    public static String buildRaw(String category, String groupKey, String line, String selectionCode) {
        return String.join("|",
                nullToEmpty(category),
                nullToEmpty(groupKey),
                nullToEmpty(line),
                nullToEmpty(selectionCode));
    }

    public static void enrichGroups(java.util.List<OddsMarketGroup> groups) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            if (group.getRows() == null) {
                continue;
            }
            OddsMarketCategory category;
            try {
                category = OddsMarketCategory.valueOf(group.getCategory());
            } catch (Exception e) {
                category = OddsMarketCategory.OTHER;
            }
            for (OddsLineRow row : group.getRows()) {
                row.setSelectionKey(build(category, group.getGroupKey(), row));
                applyBestOdds(row);
            }
        }
    }

    public static void applyBestOdds(OddsLineRow row) {
        if (row.getBookmakerOdds() == null || row.getBookmakerOdds().isEmpty()) {
            return;
        }
        String bestBk = null;
        double bestVal = -1;
        for (Map.Entry<String, String> e : row.getBookmakerOdds().entrySet()) {
            double v = parseOdds(e.getValue());
            if (v > bestVal) {
                bestVal = v;
                bestBk = e.getKey();
            }
        }
        if (bestBk != null) {
            row.setBestBookmaker(bestBk);
            row.setBestOdds(row.getBookmakerOdds().get(bestBk));
        }
    }

    private static double parseOdds(String raw) {
        if (raw == null || raw.isBlank() || "—".equals(raw.trim())) {
            return -1;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
