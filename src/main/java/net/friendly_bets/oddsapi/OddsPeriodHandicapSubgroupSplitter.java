package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Группирует форы 1-го и 2-го тайма в родительскую категорию с подгруппами.
 */
public final class OddsPeriodHandicapSubgroupSplitter {

    private record SubgroupDef(
            String groupKey,
            int codeMin,
            int codeMax
    ) {
    }

    private static final List<SubgroupDef> ORDERED_SUBGROUPS = List.of(
            new SubgroupDef("firstHalfCorrectScore", 2401, 2499),
            new SubgroupDef("secondHalfCorrectScore", 2501, 2599)
    );

    private OddsPeriodHandicapSubgroupSplitter() {
    }

    public static void splitIntoSubgroups(List<OddsMarketGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        List<OddsLineRow> periodRows = collectRows(groups, OddsMarketCategory.PERIOD_HANDICAP);
        if (periodRows.isEmpty()) {
            return;
        }
        groups.removeIf(OddsPeriodHandicapSubgroupSplitter::isPeriodHandicapGroup);

        List<OddsMarketGroup> subgroups = new ArrayList<>();
        int idx = 0;
        int baseSort = OddsMarketCategory.PERIOD_HANDICAP.getSortOrder();
        for (SubgroupDef def : ORDERED_SUBGROUPS) {
            OddsMarketGroup child = buildSubgroup(def, periodRows, baseSort, idx++);
            if (child != null) {
                subgroups.add(child);
            }
        }
        if (subgroups.isEmpty()) {
            return;
        }
        groups.add(OddsMarketGroup.builder()
                .category(OddsMarketCategory.PERIOD_HANDICAP.name())
                .groupKey("periodHandicap")
                .sortOrder(baseSort)
                .collapsedByDefault(true)
                .rows(new ArrayList<>())
                .subgroups(subgroups)
                .build());
    }

    private static OddsMarketGroup buildSubgroup(
            SubgroupDef def,
            List<OddsLineRow> source,
            int baseSort,
            int index
    ) {
        List<OddsLineRow> rows = source.stream()
                .filter(row -> codeInRange(row, def.codeMin(), def.codeMax()))
                .sorted(handicapComparator())
                .toList();
        if (rows.isEmpty()) {
            return null;
        }
        return OddsMarketGroup.builder()
                .category(OddsMarketCategory.PERIOD_HANDICAP.name())
                .groupKey(def.groupKey())
                .sortOrder(baseSort * 100 + index)
                .collapsedByDefault(true)
                .rows(new ArrayList<>(rows))
                .build();
    }

    private static Comparator<OddsLineRow> handicapComparator() {
        return Comparator
                .comparingDouble((OddsLineRow r) -> Math.abs(parseLine(r)))
                .thenComparingDouble(OddsPeriodHandicapSubgroupSplitter::parseLine)
                .thenComparingInt(r -> "AWAY".equals(r.getSelectionCode()) ? 2 : 1);
    }

    private static double parseLine(OddsLineRow row) {
        if (row == null || row.getLine() == null || row.getLine().isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(row.getLine().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isPeriodHandicapGroup(OddsMarketGroup group) {
        if (group == null) {
            return false;
        }
        if (OddsMarketCategory.PERIOD_HANDICAP.name().equals(group.getCategory())) {
            return true;
        }
        String key = group.getGroupKey();
        if (key == null) {
            return false;
        }
        return "periodHandicap".equals(key)
                || "firstHalfCorrectScore".equals(key)
                || "secondHalfCorrectScore".equals(key);
    }

    private static boolean codeInRange(OddsLineRow row, int min, int max) {
        BetTitle betTitle = row.getBetTitle();
        if (betTitle == null) {
            return false;
        }
        short code = betTitle.getCode();
        return code >= min && code <= max;
    }

    private static List<OddsLineRow> collectRows(List<OddsMarketGroup> groups, OddsMarketCategory category) {
        List<OddsLineRow> rows = new ArrayList<>();
        collectRowsRecursive(groups, category, rows);
        return rows;
    }

    private static void collectRowsRecursive(
            List<OddsMarketGroup> groups,
            OddsMarketCategory category,
            List<OddsLineRow> rows
    ) {
        if (groups == null) {
            return;
        }
        for (OddsMarketGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (category.name().equals(group.getCategory()) && group.getRows() != null) {
                rows.addAll(group.getRows());
            }
            if (group.getSubgroups() != null) {
                collectRowsRecursive(group.getSubgroups(), category, rows);
            }
        }
    }
}
