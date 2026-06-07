package net.friendly_bets.oddsapi;

import net.friendly_bets.models.BetTitle;
import net.friendly_bets.models.odds.OddsLineRow;
import net.friendly_bets.models.odds.OddsMarketGroup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Группирует «Результат + ТБ/ТМ» в родительскую категорию с подгруппами как в {@code betTitleGroups}.
 */
public final class OddsResultTotalSubgroupSplitter {

    private record SubgroupDef(
            String groupKey,
            int codeMin,
            int codeMax,
            OddsMarketCategory category
    ) {
    }

    /**
     * Порядок как в «Внести ставку»: П1 ТМ/ТБ, 1X, X, П2, X2, 12.
     */
    private static final List<SubgroupDef> ORDERED_SUBGROUPS = List.of(
            new SubgroupDef("resultTotalP1Under", 201, 250, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotalP1Over", 251, 300, OddsMarketCategory.RESULT_TOTAL_OVER),
            new SubgroupDef("resultTotal1xUnder", 301, 350, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotal1xOver", 351, 400, OddsMarketCategory.RESULT_TOTAL_OVER),
            new SubgroupDef("resultTotalDrawUnder", 401, 450, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotalDrawOver", 451, 500, OddsMarketCategory.RESULT_TOTAL_OVER),
            new SubgroupDef("resultTotalP2Under", 501, 550, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotalP2Over", 551, 600, OddsMarketCategory.RESULT_TOTAL_OVER),
            new SubgroupDef("resultTotalX2Under", 601, 650, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotalX2Over", 651, 700, OddsMarketCategory.RESULT_TOTAL_OVER),
            new SubgroupDef("resultTotal12Under", 701, 750, OddsMarketCategory.RESULT_TOTAL_UNDER),
            new SubgroupDef("resultTotal12Over", 751, 800, OddsMarketCategory.RESULT_TOTAL_OVER)
    );

    private OddsResultTotalSubgroupSplitter() {
    }

    public static void splitIntoSubgroups(List<OddsMarketGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        List<OddsLineRow> underRows = collectRows(groups, OddsMarketCategory.RESULT_TOTAL_UNDER);
        List<OddsLineRow> overRows = collectRows(groups, OddsMarketCategory.RESULT_TOTAL_OVER);
        if (underRows.isEmpty() && overRows.isEmpty()) {
            return;
        }
        groups.removeIf(OddsResultTotalSubgroupSplitter::isResultTotalGroup);

        List<OddsMarketGroup> subgroups = new ArrayList<>();
        int idx = 0;
        int baseSort = OddsMarketCategory.RESULT_TOTAL_OVER.getSortOrder();
        for (SubgroupDef def : ORDERED_SUBGROUPS) {
            List<OddsLineRow> source = def.category() == OddsMarketCategory.RESULT_TOTAL_UNDER ? underRows : overRows;
            OddsMarketGroup child = buildSubgroup(def, source, baseSort, idx++);
            if (child != null) {
                subgroups.add(child);
            }
        }
        if (subgroups.isEmpty()) {
            return;
        }
        groups.add(OddsMarketGroup.builder()
                .category("RESULT_TOTAL")
                .groupKey("resultTotal")
                .sortOrder(baseSort)
                .collapsedByDefault(true)
                .rows(new ArrayList<>())
                .subgroups(subgroups)
                .build());
        OddsResultTotalEnricher.sortGroups(groups);
    }

    private static OddsMarketGroup buildSubgroup(
            SubgroupDef def,
            List<OddsLineRow> source,
            int baseSort,
            int index
    ) {
        List<OddsLineRow> rows = source.stream()
                .filter(row -> codeInRange(row, def.codeMin(), def.codeMax()))
                .sorted(lineComparator())
                .toList();
        if (rows.isEmpty()) {
            return null;
        }
        return OddsMarketGroup.builder()
                .category(def.category().name())
                .groupKey(def.groupKey())
                .sortOrder(baseSort * 100 + index)
                .collapsedByDefault(true)
                .rows(new ArrayList<>(rows))
                .build();
    }

    private static boolean isResultTotalGroup(OddsMarketGroup group) {
        if (group == null) {
            return false;
        }
        if (isResultTotalCategory(group.getCategory())) {
            return true;
        }
        String key = group.getGroupKey();
        if (key == null) {
            return false;
        }
        if ("resultTotal".equals(key)) {
            return true;
        }
        if ("resultTotalOver".equals(key) || "resultTotalUnder".equals(key)) {
            return true;
        }
        return key.startsWith("resultTotal");
    }

    private static Comparator<OddsLineRow> lineComparator() {
        return Comparator.comparingDouble(OddsResultTotalSubgroupSplitter::lineFromRow);
    }

    private static double lineFromRow(OddsLineRow row) {
        if (row.getLine() != null && !row.getLine().isBlank()) {
            try {
                return Double.parseDouble(row.getLine().trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return 0;
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

    private static boolean isResultTotalCategory(String category) {
        return OddsMarketCategory.RESULT_TOTAL_OVER.name().equals(category)
                || OddsMarketCategory.RESULT_TOTAL_UNDER.name().equals(category)
                || "RESULT_TOTAL".equals(category);
    }
}
