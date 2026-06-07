package net.friendly_bets.oddsapi;

import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import net.friendly_bets.oddsapi.mapping.BetTitleKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Схлопывает дубли строк в prod-merge (один BetTitle → одна строка).
 * Marathon SSE может отдавать несколько {@code MTCH_HB} на одну линию; при persist остаётся один снимок на матч.
 */
public final class OddsLineRowDeduper {

    private OddsLineRowDeduper() {
    }

    public static void dedupeMarketGroups(List<OddsMarketGroup> groups) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            dedupeGroup(group);
        }
    }

    private static void dedupeGroup(OddsMarketGroup group) {
        if (group == null) {
            return;
        }
        if (group.getRows() != null && !group.getRows().isEmpty()) {
            group.setRows(dedupeRows(group.getRows()));
        }
        if (group.getSubgroups() != null) {
            for (OddsMarketGroup sub : group.getSubgroups()) {
                dedupeGroup(sub);
            }
        }
    }

    public static List<OddsLineRow> dedupeRows(List<OddsLineRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows != null ? rows : List.of();
        }
        return dedupeByDisplayLabel(dedupeByBetTitle(rows));
    }

    private static List<OddsLineRow> dedupeByBetTitle(List<OddsLineRow> rows) {
        Map<BetTitleKey, OddsLineRow> byKey = new LinkedHashMap<>();
        List<OddsLineRow> withoutKey = new ArrayList<>();
        for (OddsLineRow row : rows) {
            BetTitleKey key = BetTitleKey.from(row.getBetTitle());
            if (key == null) {
                withoutKey.add(row);
                continue;
            }
            OddsLineRow existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, row);
            } else {
                mergeInto(existing, row);
            }
        }
        List<OddsLineRow> result = new ArrayList<>(byKey.values());
        result.addAll(withoutKey);
        return result;
    }

    /**
     * Несколько MTCH_HB на одну линию иногда дают разные BetTitle при одинаковой подписи в UI.
     */
    private static List<OddsLineRow> dedupeByDisplayLabel(List<OddsLineRow> rows) {
        Map<String, OddsLineRow> byLabel = new LinkedHashMap<>();
        List<OddsLineRow> withoutLabel = new ArrayList<>();
        for (OddsLineRow row : rows) {
            String label = row.getDisplayLabel();
            if (label == null || label.isBlank()) {
                withoutLabel.add(row);
                continue;
            }
            String key = label.trim().toLowerCase(Locale.ROOT);
            OddsLineRow existing = byLabel.get(key);
            if (existing == null) {
                byLabel.put(key, row);
            } else {
                mergeInto(existing, row);
            }
        }
        List<OddsLineRow> result = new ArrayList<>(byLabel.values());
        result.addAll(withoutLabel);
        return result;
    }

    /**
     * Поздняя строка перезаписывает кэфы (актуальный SSE-снимок, не накопление истории).
     */
    private static void mergeInto(OddsLineRow target, OddsLineRow incoming) {
        if (incoming.getBookmakerOdds() != null) {
            if (target.getBookmakerOdds() == null) {
                target.setBookmakerOdds(new LinkedHashMap<>());
            }
            target.getBookmakerOdds().putAll(incoming.getBookmakerOdds());
        }
        if (incoming.getBookmakerSourcePaths() != null) {
            if (target.getBookmakerSourcePaths() == null) {
                target.setBookmakerSourcePaths(new LinkedHashMap<>());
            }
            for (Map.Entry<String, String> e : incoming.getBookmakerSourcePaths().entrySet()) {
                if (incoming.getBookmakerOdds() != null
                        && incoming.getBookmakerOdds().get(e.getKey()) != null) {
                    target.getBookmakerSourcePaths().put(e.getKey(), e.getValue());
                }
            }
        }
        if (incoming.getDisplayLabel() != null && !incoming.getDisplayLabel().isBlank()) {
            target.setDisplayLabel(incoming.getDisplayLabel());
        }
        if (incoming.getLine() != null) {
            target.setLine(incoming.getLine());
        }
        if (incoming.getSelectionCode() != null) {
            target.setSelectionCode(incoming.getSelectionCode());
        }
        target.setCrossBookmakerMismatch(incoming.isCrossBookmakerMismatch());
        OddsSelectionKey.applyBestOdds(target);
    }
}
